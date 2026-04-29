package com.zzp.pvms.auth.domain.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String applicant; // 对应 SysUser 的 nickname
    private String reason;    // 理由，可以存入备注或临时记录
    private String captcha;
}
