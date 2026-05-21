package com.zzp.pvms.auth.domain.vo;

import com.zzp.pvms.auth.domain.SysRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Data
public class RoleVO {
    private Long id;
    private String roleKey;
    private String roleName;
    private Integer status;
    private Boolean superAdmin;
    private Boolean visitor;
    private List<String> menuKeys;
    private List<Long> directoryIds;
    private List<Long> albumIds;
    private List<Long> tagIds;
    private String remark;
    private Boolean builtin;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static RoleVO from(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleKey(role.getRoleKey());
        vo.setRoleName(role.getRoleName());
        vo.setStatus(role.getStatus());
        vo.setSuperAdmin(Integer.valueOf(1).equals(role.getSuperAdmin()));
        vo.setVisitor(Integer.valueOf(1).equals(role.getVisitor()));
        vo.setMenuKeys(splitStrings(role.getMenuKeys()));
        vo.setDirectoryIds(splitLongs(role.getDirectoryIds()));
        vo.setAlbumIds(splitLongs(role.getAlbumIds()));
        vo.setTagIds(splitLongs(role.getTagIds()));
        vo.setRemark(role.getRemark());
        vo.setBuiltin(Integer.valueOf(1).equals(role.getBuiltin()));
        vo.setCreateTime(role.getCreateTime());
        vo.setUpdateTime(role.getUpdateTime());
        return vo;
    }

    private static List<String> splitStrings(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private static List<Long> splitLongs(String value) {
        return splitStrings(value).stream().map(Long::valueOf).toList();
    }
}
