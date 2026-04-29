package com.zzp.pvms.media.mq;

import com.zzp.pvms.common.constant.MqConstants;
import com.zzp.pvms.common.dto.MediaProcessMsg;
import com.zzp.pvms.media.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MediaFileConsumer {

    @Autowired
    private MediaFileService mediaFileService;

    @RabbitListener(queues = MqConstants.METADATA_QUEUE)
    public void handleMetadataExtraction(MediaProcessMsg msg) {
        log.info("接收到元数据提取任务: {}", msg.getFilePath());
        try {
            mediaFileService.processMetadata(msg);
        } catch (Exception e) {
            log.error("元数据提取任务处理失败: {}", msg.getFilePath(), e);
        }
    }
}
