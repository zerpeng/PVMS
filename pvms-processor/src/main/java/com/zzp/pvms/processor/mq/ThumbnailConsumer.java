package com.zzp.pvms.processor.mq;

import com.zzp.pvms.common.constant.MqConstants;
import com.zzp.pvms.common.dto.MediaProcessMsg;
import com.zzp.pvms.processor.service.ThumbnailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ThumbnailConsumer {

    @Autowired
    private ThumbnailService thumbnailService;

    @RabbitListener(queues = MqConstants.THUMBNAIL_QUEUE)
    public void onMessage(MediaProcessMsg msg) {
        log.info("接收到缩略图生成任务: mediaId={}, path={}", msg.getMediaId(), msg.getFilePath());
        try {
            thumbnailService.createThumbnail(msg);
        } catch (Exception e) {
            log.error("处理缩略图任务异常: {}", msg.getFilePath(), e);
        }
    }
}
