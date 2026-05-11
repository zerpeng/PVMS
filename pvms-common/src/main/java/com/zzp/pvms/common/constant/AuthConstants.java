package com.zzp.pvms.common.constant;

/**
 * 认证相关公共常量。
 */
public interface AuthConstants {

    /**
     * Redis 中 JWT 黑名单 key 前缀，退出登录后 token 会写入该前缀。
     */
    String JWT_BLACKLIST_PREFIX = "pvms:auth:jwt:blacklist:";
}
