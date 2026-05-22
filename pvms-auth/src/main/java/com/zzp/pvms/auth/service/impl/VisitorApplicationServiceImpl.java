package com.zzp.pvms.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.auth.domain.SysRole;
import com.zzp.pvms.auth.domain.SysUser;
import com.zzp.pvms.auth.domain.SysVisitorApplication;
import com.zzp.pvms.auth.domain.dto.AuditApplicationRequest;
import com.zzp.pvms.auth.domain.dto.RegisterRequest;
import com.zzp.pvms.auth.domain.vo.VisitorApplicationVO;
import com.zzp.pvms.auth.mapper.SysRoleMapper;
import com.zzp.pvms.auth.mapper.SysUserMapper;
import com.zzp.pvms.auth.mapper.VisitorApplicationMapper;
import com.zzp.pvms.auth.service.VisitorApplicationService;
import com.zzp.pvms.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class VisitorApplicationServiceImpl extends ServiceImpl<VisitorApplicationMapper, SysVisitorApplication>
        implements VisitorApplicationService {
    private static final List<String> ADMIN_MENUS = List.of("library", "projects", "album-tags", "favorites", "users", "roles", "applications");
    private static final List<String> VISITOR_MENUS = List.of("library", "album-tags");

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public void createFromRegister(RegisterRequest request, SysUser user) {
        SysVisitorApplication application = new SysVisitorApplication();
        application.setUserId(user.getId());
        application.setUsername(user.getUsername());
        application.setApplicantName(trimToNull(request.getApplicant()));
        application.setReason(trimToNull(request.getReason()));
        application.setStatus(0);
        application.setCreateTime(LocalDateTime.now());
        save(application);
    }

    @Override
    public List<VisitorApplicationVO> list(Integer status) {
        return list(new LambdaQueryWrapper<SysVisitorApplication>()
                .eq(status != null, SysVisitorApplication::getStatus, status)
                .orderByDesc(SysVisitorApplication::getCreateTime)
                .orderByDesc(SysVisitorApplication::getId))
                .stream()
                .map(VisitorApplicationVO::from)
                .toList();
    }

    @Override
    public long pendingCount() {
        return count(new LambdaQueryWrapper<SysVisitorApplication>().eq(SysVisitorApplication::getStatus, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VisitorApplicationVO audit(Long id, AuditApplicationRequest request, String auditor) {
        SysVisitorApplication application = getById(id);
        if (application == null) {
            throw new ApiException("访问申请不存在");
        }
        if (!Integer.valueOf(0).equals(application.getStatus())) {
            throw new ApiException("该申请已审核");
        }

        boolean approved = Boolean.TRUE.equals(request.getApproved());
        SysUser user = resolveUser(application);
        LocalDateTime now = LocalDateTime.now();
        application.setAuditUsername(trimToNull(auditor));
        application.setAuditRemark(trimToNull(request.getRemark()));
        application.setAuditTime(now);

        if (approved) {
            String roleKey = "APP_" + application.getId();
            upsertRole(roleKey, application, request, now);
            user.setRoleKey(roleKey);
            user.setStatus(1);
            user.setUpdateTime(now);
            sysUserMapper.updateById(user);

            application.setStatus(1);
            application.setRoleKey(roleKey);
        } else {
            user.setStatus(0);
            user.setUpdateTime(now);
            sysUserMapper.updateById(user);
            application.setStatus(2);
        }

        updateById(application);
        return VisitorApplicationVO.from(getById(application.getId()));
    }

    private SysUser resolveUser(SysVisitorApplication application) {
        SysUser user = application.getUserId() == null ? null : sysUserMapper.selectById(application.getUserId());
        if (user == null && StringUtils.hasText(application.getUsername())) {
            user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, application.getUsername())
                    .last("LIMIT 1"));
        }
        if (user == null) {
            throw new ApiException("申请对应用户不存在");
        }
        return user;
    }

    private void upsertRole(String roleKey, SysVisitorApplication application, AuditApplicationRequest request, LocalDateTime now) {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, roleKey)
                .last("LIMIT 1"));
        boolean create = role == null;
        if (create) {
            role = new SysRole();
            role.setRoleKey(roleKey);
            role.setBuiltin(0);
            role.setCreateTime(now);
        }

        boolean superAdmin = Boolean.TRUE.equals(request.getSuperAdmin());
        boolean visitor = request.getVisitor() == null || Boolean.TRUE.equals(request.getVisitor());
        role.setRoleName(resolveRoleName(application, request));
        role.setStatus(1);
        role.setSuperAdmin(superAdmin ? 1 : 0);
        role.setVisitor(visitor ? 1 : 0);
        role.setMenuKeys(joinStrings(superAdmin ? ADMIN_MENUS : visitor ? VISITOR_MENUS : request.getMenuKeys()));
        role.setDirectoryIds(superAdmin ? "" : joinLongs(request.getDirectoryIds()));
        role.setAlbumIds(superAdmin ? "" : joinLongs(request.getAlbumIds()));
        role.setTagIds(superAdmin ? "" : joinLongs(request.getTagIds()));
        role.setRemark("访问申请审核生成：" + nullToEmpty(application.getUsername()));
        role.setUpdateTime(now);

        if (create) {
            sysRoleMapper.insert(role);
        } else {
            sysRoleMapper.updateById(role);
        }
    }

    private String resolveRoleName(SysVisitorApplication application, AuditApplicationRequest request) {
        if (StringUtils.hasText(request.getRoleName())) {
            return request.getRoleName().trim();
        }
        String name = StringUtils.hasText(application.getApplicantName()) ? application.getApplicantName() : application.getUsername();
        return nullToEmpty(name) + "访问权限";
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

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
