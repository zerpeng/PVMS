package com.zzp.pvms.auth.controller;

import com.zzp.pvms.auth.domain.dto.LoginRequest;
import com.zzp.pvms.auth.service.CaptchaService;
import com.zzp.pvms.auth.service.LogoutService;
import com.zzp.pvms.auth.util.JwtTokenUtil;
import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.common.constant.ServiceConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.zzp.pvms.auth.mapper.SysUserMapper sysUserMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private LogoutService logoutService;

    /**
     * 获取图形验证码
     */
    @org.springframework.web.bind.annotation.GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        // 生成验证码图片和服务端校验 key，前端登录时需要把 key 一起带回。
        CaptchaService.CaptchaPayload captcha = captchaService.generate();
        Map<String, String> result = new java.util.HashMap<>();
        result.put("img", captcha.getImg());
        result.put("key", captcha.getKey());

        return Result.success("验证码获取成功", result);
    }

    /**
     * 访客注册（申请）
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody com.zzp.pvms.auth.domain.dto.RegisterRequest request) {
        log.info("用户注册申请: {}", request.getUsername());
        
        // 1. 检查重复
        Long count = sysUserMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.zzp.pvms.auth.domain.SysUser>()
                .eq(com.zzp.pvms.auth.domain.SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            return Result.failed("用户名已存在");
        }

        // 2. 创建用户 (初始状态为禁用，角色为 VISITOR)
        com.zzp.pvms.auth.domain.SysUser user = new com.zzp.pvms.auth.domain.SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getApplicant());
        user.setRoleKey("VISITOR");
        user.setStatus(0); // 需管理员审批
        user.setCreateTime(java.time.LocalDateTime.now());
        
        sysUserMapper.insert(user);
        
        log.info("注册申请成功: {}", request.getUsername());
        return Result.success("申请已提交，请等待管理员审批");
    }

    @PostMapping("/login")
    public Result<String> login(@jakarta.validation.Valid @RequestBody LoginRequest loginRequest) {
        log.info("用户登录尝试: {}, 密码长度: {}", loginRequest.getUsername(), 
                loginRequest.getPassword() != null ? loginRequest.getPassword().length() : 0);
        try {
            // 先校验验证码，拦截无效登录请求，避免继续触发用户名密码认证。
            if (!captchaService.validate(loginRequest.getCaptchaKey(), loginRequest.getCaptcha())) {
                return Result.failed("验证码错误或已过期");
            }

            // 1. 执行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
            
            // 2. 认证成功，生成 Token
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtTokenUtil.generateToken(userDetails);
            
            log.info("用户登录成功: {}, 生成 Token: {}", loginRequest.getUsername(), token.substring(0, 10) + "...");
            return Result.success("登录成功", token);
        } catch (Exception e) {
            log.error("用户登录失败: {}, 原因: {}", loginRequest.getUsername(), e.getMessage());
            return Result.failed("登录失败: " + e.getMessage());
        }
    }

    /**
     * 退出登录，将当前 JWT 写入 Redis 黑名单，后续请求会被网关拒绝。
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = ServiceConstants.AUTHORIZATION_HEADER, required = false) String authorization) {
        logoutService.logout(authorization);
        return Result.success("退出登录成功");
    }
}
