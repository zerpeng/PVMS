package com.zzp.pvms.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 用户输入的图形验证码文本。
     */
    @NotBlank(message = "验证码不能为空")
    private String captcha;

    /**
     * 后端生成验证码时返回的唯一标识，用于定位服务端缓存中的验证码答案。
     */
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;
}
