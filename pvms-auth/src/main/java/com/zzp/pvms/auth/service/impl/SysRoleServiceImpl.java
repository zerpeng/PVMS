package com.zzp.pvms.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.auth.domain.SysRole;
import com.zzp.pvms.auth.domain.dto.RoleRequest;
import com.zzp.pvms.auth.domain.vo.RoleVO;
import com.zzp.pvms.auth.mapper.SysRoleMapper;
import com.zzp.pvms.auth.service.SysRoleService;
import com.zzp.pvms.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
    private static final List<String> ADMIN_MENUS = List.of("library", "projects", "album-tags", "favorites", "users", "roles");
    private static final List<String> USER_MENUS = List.of("library", "projects", "album-tags", "favorites");
    private static final List<String> VISITOR_MENUS = List.of("library", "album-tags");

    @Override
    public List<RoleVO> listRoles() {
        ensureDefaultRoles();
        return list(new LambdaQueryWrapper<SysRole>()
                .orderByDesc(SysRole::getBuiltin)
                .orderByAsc(SysRole::getId))
                .stream()
                .map(RoleVO::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO create(RoleRequest request) {
        String roleKey = normalizeRoleKey(request.getRoleKey());
        if (!StringUtils.hasText(roleKey)) {
            throw new ApiException("角色标识不能为空");
        }
        long duplicateCount = count(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, roleKey));
        if (duplicateCount > 0) {
            throw new ApiException("角色标识已存在");
        }

        SysRole role = new SysRole();
        role.setRoleKey(roleKey);
        role.setBuiltin(0);
        applyChanges(role, request);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        save(role);
        return RoleVO.from(getById(role.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO update(Long id, RoleRequest request) {
        SysRole role = getById(id);
        if (role == null) {
            throw new ApiException("角色不存在");
        }

        if (!Integer.valueOf(1).equals(role.getBuiltin()) && StringUtils.hasText(request.getRoleKey())) {
            String nextRoleKey = normalizeRoleKey(request.getRoleKey());
            long duplicateCount = count(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleKey, nextRoleKey)
                    .ne(SysRole::getId, id));
            if (duplicateCount > 0) {
                throw new ApiException("角色标识已存在");
            }
            role.setRoleKey(nextRoleKey);
        }

        applyChanges(role, request);
        role.setUpdateTime(LocalDateTime.now());
        updateById(role);
        return RoleVO.from(getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long id) {
        SysRole role = getById(id);
        if (role == null) {
            return false;
        }
        if (Integer.valueOf(1).equals(role.getBuiltin())) {
            throw new ApiException("内置角色不能删除");
        }
        return removeById(id);
    }

    @Override
    public SysRole getActiveRole(String roleKey) {
        ensureDefaultRoles();
        if (!StringUtils.hasText(roleKey)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, normalizeRoleKey(roleKey))
                .eq(SysRole::getStatus, 1)
                .last("LIMIT 1"));
    }

    private void ensureDefaultRoles() {
        ensureRole("ADMIN", "超级管理员", 1, 1, 0, ADMIN_MENUS);
        ensureRole("USER", "普通用户", 1, 0, 0, USER_MENUS);
        ensureRole("VISITOR", "访客", 1, 0, 1, VISITOR_MENUS);
    }

    private void ensureRole(String roleKey, String roleName, int status, int superAdmin, int visitor, List<String> menuKeys) {
        SysRole existing = getOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, roleKey).last("LIMIT 1"));
        if (existing != null) {
            return;
        }

        SysRole role = new SysRole();
        role.setRoleKey(roleKey);
        role.setRoleName(roleName);
        role.setStatus(status);
        role.setSuperAdmin(superAdmin);
        role.setVisitor(visitor);
        role.setMenuKeys(joinStrings(menuKeys));
        role.setDirectoryIds("");
        role.setAlbumIds("");
        role.setTagIds("");
        role.setBuiltin(1);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        save(role);
    }

    private void applyChanges(SysRole role, RoleRequest request) {
        String roleName = normalizeText(request.getRoleName());
        if (roleName == null) {
            throw new ApiException("角色名称不能为空");
        }

        boolean superAdmin = Boolean.TRUE.equals(request.getSuperAdmin());
        boolean visitor = Boolean.TRUE.equals(request.getVisitor());
        role.setRoleName(roleName);
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        role.setSuperAdmin(superAdmin ? 1 : 0);
        role.setVisitor(visitor ? 1 : 0);
        role.setMenuKeys(joinStrings(superAdmin ? ADMIN_MENUS : visitor ? VISITOR_MENUS : request.getMenuKeys()));
        role.setDirectoryIds(superAdmin ? "" : joinLongs(request.getDirectoryIds()));
        role.setAlbumIds(superAdmin ? "" : joinLongs(request.getAlbumIds()));
        role.setTagIds(superAdmin ? "" : joinLongs(request.getTagIds()));
        role.setRemark(normalizeText(request.getRemark()));
    }

    private String normalizeRoleKey(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String joinStrings(Collection<String> values) {
        if (values == null) {
            return "";
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private String joinLongs(Collection<Long> values) {
        if (values == null) {
            return "";
        }
        return values.stream()
                .filter(Objects::nonNull)
                .filter(value -> value > 0)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
