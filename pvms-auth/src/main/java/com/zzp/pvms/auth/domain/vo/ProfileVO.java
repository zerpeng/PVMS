package com.zzp.pvms.auth.domain.vo;

import com.zzp.pvms.auth.domain.SysRole;
import com.zzp.pvms.auth.domain.SysUser;
import lombok.Data;

@Data
public class ProfileVO {
    private Long id;
    private String username;
    private String nickname;
    private String roleKey;
    private String roleName;
    private Boolean superAdmin;
    private Boolean visitor;

    public static ProfileVO from(SysUser user, SysRole role) {
        ProfileVO vo = new ProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRoleKey(user.getRoleKey());
        vo.setRoleName(role == null ? user.getRoleKey() : role.getRoleName());
        vo.setSuperAdmin(role != null && Integer.valueOf(1).equals(role.getSuperAdmin()));
        vo.setVisitor(role != null && Integer.valueOf(1).equals(role.getVisitor()));
        return vo;
    }
}
