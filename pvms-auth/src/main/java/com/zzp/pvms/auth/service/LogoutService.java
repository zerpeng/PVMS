package com.zzp.pvms.auth.service;

import com.zzp.pvms.auth.util.JwtTokenUtil;
import com.zzp.pvms.common.constant.AuthConstants;
import com.zzp.pvms.common.constant.ServiceConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;

/**
 * 退出登录服务，负责将仍未过期的 JWT 写入 Redis 黑名单。
 */
@Service
public class LogoutService {

    private final JwtTokenUtil jwtTokenUtil;
    private final StringRedisTemplate stringRedisTemplate;

    public LogoutService(JwtTokenUtil jwtTokenUtil, StringRedisTemplate stringRedisTemplate) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 退出登录时将当前 token 加入黑名单，直到原 token 过期后由 Redis 自动清理。
     */
    public void logout(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(ServiceConstants.TOKEN_PREFIX)) {
            return;
        }

        String token = authorization.substring(ServiceConstants.TOKEN_PREFIX.length());
        Date expiration = jwtTokenUtil.extractExpiration(token);
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }

        stringRedisTemplate.opsForValue().set(
                AuthConstants.JWT_BLACKLIST_PREFIX + token,
                "1",
                Duration.ofMillis(ttlMillis)
        );
    }
}
