package com.zzp.pvms.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzp.pvms.auth.domain.SysRole;
import com.zzp.pvms.auth.domain.SysUser;
import com.zzp.pvms.auth.domain.dto.LoginRequest;
import com.zzp.pvms.auth.domain.dto.RegisterRequest;
import com.zzp.pvms.auth.domain.vo.ProfileVO;
import com.zzp.pvms.auth.mapper.SysUserMapper;
import com.zzp.pvms.auth.service.CaptchaService;
import com.zzp.pvms.auth.service.LogoutService;
import com.zzp.pvms.auth.service.SysRoleService;
import com.zzp.pvms.auth.service.VisitorApplicationService;
import com.zzp.pvms.auth.util.JwtTokenUtil;
import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.common.constant.ServiceConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private LogoutService logoutService;

    @Autowired
    private VisitorApplicationService visitorApplicationService;

    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        CaptchaService.CaptchaPayload captcha = captchaService.generate();
        Map<String, String> result = new HashMap<>();
        result.put("img", captcha.getImg());
        result.put("key", captcha.getKey());
        return Result.success("验证码获取成功", result);
    }

    @PostMapping("/register")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> register(@RequestBody RegisterRequest request) {
        if (!captchaService.validate(request.getCaptchaKey(), request.getCaptcha())) {
            return Result.failed("验证码错误或已过期");
        }
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())
                || !StringUtils.hasText(request.getApplicant())) {
            return Result.failed("用户名、密码和真实姓名不能为空");
        }

        String username = request.getUsername().trim();
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (count > 0) {
            return Result.failed("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getApplicant().trim());
        user.setRoleKey("VISITOR");
        user.setStatus(0);
        user.setDelFlag(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);

        visitorApplicationService.createFromRegister(request, user);
        log.info("访问申请已提交: {}", username);
        return Result.success("申请已提交，请等待管理员审核");
    }

    @PostMapping("/login")
    public Result<String> login(@jakarta.validation.Valid @RequestBody LoginRequest loginRequest) {
        try {
            if (!captchaService.validate(loginRequest.getCaptchaKey(), loginRequest.getCaptcha())) {
                return Result.failed("验证码错误或已过期");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtTokenUtil.generateToken(userDetails);
            return Result.success("登录成功", token);
        } catch (Exception e) {
            log.warn("用户登录失败: {}, reason={}", loginRequest.getUsername(), e.getMessage());
            return Result.failed("登录失败: " + e.getMessage());
        }
    }

    @GetMapping("/profile")
    public Result<ProfileVO> profile(@RequestHeader(value = "X-User-Name", required = false) String username) {
        if (!StringUtils.hasText(username)) {
            return Result.failed(401, "未登录或登录已过期");
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
        if (user == null) {
            return Result.failed("用户不存在");
        }

        SysRole role = sysRoleService.getActiveRole(user.getRoleKey());
        return Result.success(ProfileVO.from(user, role));
    }

    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = ServiceConstants.AUTHORIZATION_HEADER, required = false) String authorization) {
        logoutService.logout(authorization);
        return Result.success("退出登录成功");
    }
}
