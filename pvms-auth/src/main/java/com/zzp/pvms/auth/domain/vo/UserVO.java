package com.zzp.pvms.auth.domain.vo;

import com.zzp.pvms.auth.domain.SysUser;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String roleKey;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static UserVO from(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRoleKey(user.getRoleKey());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }
}
