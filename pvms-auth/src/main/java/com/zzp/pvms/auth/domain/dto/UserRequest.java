package com.zzp.pvms.auth.domain.dto;

import lombok.Data;

@Data
public class UserRequest {
    private String username;
    private String password;
    private String nickname;
    private String roleKey;
    private Integer status;
}
