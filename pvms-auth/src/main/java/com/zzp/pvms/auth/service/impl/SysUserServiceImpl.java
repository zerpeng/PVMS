package com.zzp.pvms.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzp.pvms.auth.domain.SysUser;
import com.zzp.pvms.auth.domain.dto.UserRequest;
import com.zzp.pvms.auth.domain.vo.UserImportResult;
import com.zzp.pvms.auth.domain.vo.UserVO;
import com.zzp.pvms.auth.mapper.SysUserMapper;
import com.zzp.pvms.auth.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SysUserServiceImpl implements SysUserService {
    private static final String DEFAULT_ROLE_KEY = "USER";
    private static final String DEFAULT_IMPORT_PASSWORD = "123456";

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Page<UserVO> queryPage(int current, int size, String username, String nickname, String roleKey, Integer status) {
        Page<SysUser> userPage = sysUserMapper.selectPage(new Page<>(current, size), buildQuery(username, nickname, roleKey, status));
        Page<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setPages(userPage.getPages());
        voPage.setRecords(userPage.getRecords().stream().map(UserVO::from).toList());
        return voPage;
    }

    @Override
    public UserVO create(UserRequest request) {
        assertRequired(request.getUsername(), "用户名不能为空");
        assertRequired(request.getPassword(), "密码不能为空");
        ensureUsernameAvailable(request.getUsername(), null);

        SysUser user = new SysUser();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(trimToNull(request.getNickname()));
        user.setRoleKey(normalizeRoleKey(request.getRoleKey()));
        user.setStatus(normalizeStatus(request.getStatus()));
        user.setDelFlag(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);
        return UserVO.from(user);
    }

    @Override
    public UserVO update(Long id, UserRequest request) {
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        if (StringUtils.hasText(request.getUsername())) {
            ensureUsernameAvailable(request.getUsername(), id);
            existing.setUsername(request.getUsername().trim());
        }
        if (StringUtils.hasText(request.getPassword())) {
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        existing.setNickname(trimToNull(request.getNickname()));
        existing.setRoleKey(normalizeRoleKey(request.getRoleKey()));
        existing.setStatus(normalizeStatus(request.getStatus()));
        existing.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(existing);
        return UserVO.from(sysUserMapper.selectById(id));
    }

    @Override
    public boolean remove(Long id, String currentUsername) {
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        if (StringUtils.hasText(currentUsername) && currentUsername.equals(existing.getUsername())) {
            throw new IllegalArgumentException("不能删除当前登录用户");
        }
        return sysUserMapper.deleteById(id) > 0;
    }

    @Override
    public byte[] exportCsv(String username, String nickname, String roleKey, Integer status) {
        List<SysUser> users = sysUserMapper.selectList(buildQuery(username, nickname, roleKey, status));
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("id,username,nickname,roleKey,status,createTime,updateTime\n");
        for (SysUser user : users) {
            builder.append(user.getId()).append(',')
                    .append(escapeCsv(user.getUsername())).append(',')
                    .append(escapeCsv(user.getNickname())).append(',')
                    .append(escapeCsv(user.getRoleKey())).append(',')
                    .append(user.getStatus() == null ? "" : user.getStatus()).append(',')
                    .append(user.getCreateTime() == null ? "" : user.getCreateTime()).append(',')
                    .append(user.getUpdateTime() == null ? "" : user.getUpdateTime()).append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public UserImportResult importCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的 CSV 文件");
        }

        UserImportResult result = new UserImportResult();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (!StringUtils.hasText(headerLine)) {
                throw new IllegalArgumentException("CSV 文件不能为空");
            }

            Map<String, Integer> headerIndex = resolveHeader(parseCsvLine(stripBom(headerLine)));
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!StringUtils.hasText(line)) {
                    result.addSkipped();
                    continue;
                }

                try {
                    importUser(parseCsvLine(line), headerIndex, result);
                } catch (Exception e) {
                    result.addSkipped();
                    result.addError("第 " + lineNumber + " 行导入失败：" + e.getMessage());
                }
            }
        }
        return result;
    }

    private LambdaQueryWrapper<SysUser> buildQuery(String username, String nickname, String roleKey, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username)
                .like(StringUtils.hasText(nickname), SysUser::getNickname, nickname)
                .eq(StringUtils.hasText(roleKey), SysUser::getRoleKey, roleKey)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreateTime)
                .orderByDesc(SysUser::getId);
        return wrapper;
    }

    private void importUser(List<String> row, Map<String, Integer> headerIndex, UserImportResult result) {
        String username = getCsvValue(row, headerIndex, "username");
        assertRequired(username, "用户名不能为空");

        String password = getCsvValue(row, headerIndex, "password");
        String nickname = getCsvValue(row, headerIndex, "nickname");
        String roleKey = getCsvValue(row, headerIndex, "rolekey");
        String status = getCsvValue(row, headerIndex, "status");

        SysUser existing = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username.trim()));
        if (existing == null) {
            SysUser user = new SysUser();
            user.setUsername(username.trim());
            user.setPassword(passwordEncoder.encode(StringUtils.hasText(password) ? password : DEFAULT_IMPORT_PASSWORD));
            user.setNickname(trimToNull(nickname));
            user.setRoleKey(normalizeRoleKey(roleKey));
            user.setStatus(parseStatus(status));
            user.setDelFlag(0);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            sysUserMapper.insert(user);
            result.addCreated();
            return;
        }

        if (StringUtils.hasText(password)) {
            existing.setPassword(passwordEncoder.encode(password));
        }
        existing.setNickname(trimToNull(nickname));
        existing.setRoleKey(normalizeRoleKey(roleKey));
        existing.setStatus(parseStatus(status));
        existing.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(existing);
        result.addUpdated();
    }

    private void ensureUsernameAvailable(String username, Long ignoreId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim());
        if (ignoreId != null) {
            wrapper.ne(SysUser::getId, ignoreId);
        }
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }

    private void assertRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizeRoleKey(String roleKey) {
        if (!StringUtils.hasText(roleKey)) {
            return DEFAULT_ROLE_KEY;
        }
        return roleKey.trim().toUpperCase(Locale.ROOT);
    }

    private Integer normalizeStatus(Integer status) {
        return status == null ? 1 : status;
    }

    private Integer parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return 1;
        }
        return Integer.parseInt(status.trim());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String stripBom(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private Map<String, Integer> resolveHeader(List<String> header) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            index.put(header.get(i).replace("_", "").trim().toLowerCase(Locale.ROOT), i);
        }
        if (!index.containsKey("username")) {
            throw new IllegalArgumentException("CSV 表头必须包含 username");
        }
        return index;
    }

    private String getCsvValue(List<String> row, Map<String, Integer> headerIndex, String key) {
        Integer index = headerIndex.get(key);
        if (index == null || index >= row.size()) {
            return null;
        }
        return row.get(index);
    }
}
