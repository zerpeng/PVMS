package com.zzp.pvms.auth.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

@Service
public class CaptchaService {

    /**
     * Redis 中验证码 key 的统一前缀，便于排查和避免与其他业务 key 冲突。
     */
    private static final String CAPTCHA_KEY_PREFIX = "pvms:auth:captcha:";

    /**
     * 验证码有效期，超过该时间后即使输入正确也会判定为失效。
     */
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

    /**
     * Redis 字符串客户端，用于保存验证码答案并依赖 Redis 自动过期。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 通过构造器注入 Redis 客户端，便于测试和避免字段注入。
     */
    public CaptchaService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成图形验证码，并将验证码答案保存到 Redis 中。
     */
    public CaptchaPayload generate() {
        // 使用随机 key 绑定验证码答案，避免前端只传验证码文本导致无法定位校验对象。
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40);
        String key = "captcha_" + UUID.randomUUID();

        // 将验证码答案写入 Redis，并设置过期时间，过期后 Redis 会自动清理。
        stringRedisTemplate.opsForValue().set(redisKey(key), captcha.getCode(), CAPTCHA_TTL);

        return new CaptchaPayload(key, captcha.getImageBase64Data());
    }

    /**
     * 校验前端提交的验证码，校验后立即移除缓存，防止同一个验证码被重复使用。
     */
    public boolean validate(String key, String code) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(code)) {
            return false;
        }

        String redisKey = redisKey(key);

        // 先读取再删除，兼容不支持 GETDEL 命令的旧版本 Redis。
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        stringRedisTemplate.delete(redisKey);
        if (!StringUtils.hasText(cachedCode)) {
            return false;
        }

        return cachedCode.equalsIgnoreCase(code.trim());
    }

    /**
     * 拼接实际存储到 Redis 的 key。
     */
    private String redisKey(String key) {
        return CAPTCHA_KEY_PREFIX + key;
    }

    /**
     * 返回给前端的验证码数据，img 用于展示图片，key 用于登录时回传校验。
     */
    @Data
    @AllArgsConstructor
    public static class CaptchaPayload {
        private String key;
        private String img;
    }
}
