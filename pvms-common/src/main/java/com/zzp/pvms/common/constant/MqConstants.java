package com.zzp.pvms.common.constant;

/**
 * 消息队列常量
 */
public interface MqConstants {

    // 交换机
    String MEDIA_EXCHANGE = "pvms.media.exchange";

    // 队列：元数据提取
    String METADATA_QUEUE = "media.metadata.extract";
    // Routing Key：元数据提取
    String METADATA_ROUTING_KEY = "media.metadata.routing";

    // 队列：缩略图生成
    String THUMBNAIL_QUEUE = "media.thumbnail.generate";
    // Routing Key：缩略图生成
    String THUMBNAIL_ROUTING_KEY = "media.thumbnail.routing";

    // 队列：视频转码任务
    String VIDEO_PROCESS_QUEUE = "media.video.process";
    // Routing Key：视频转码
    String VIDEO_PROCESS_ROUTING_KEY = "media.video.routing";
}
