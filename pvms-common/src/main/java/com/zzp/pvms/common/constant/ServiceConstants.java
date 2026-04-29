package com.zzp.pvms.common.constant;

/**
 * 服务名称及通用常量
 */
public interface ServiceConstants {
    
    // 服务名称
    String AUTH_SERVICE = "pvms-auth";
    String MEDIA_SERVICE = "pvms-media";
    String PROCESSOR_SERVICE = "pvms-processor";
    
    // 权限相关
    String AUTHORIZATION_HEADER = "Authorization";
    String TOKEN_PREFIX = "Bearer ";
    
    // 逻辑删除
    Integer NORMAL = 0;
    Integer DELETED = 1;
}
