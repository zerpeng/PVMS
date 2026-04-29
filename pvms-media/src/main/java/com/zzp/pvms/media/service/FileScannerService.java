package com.zzp.pvms.media.service;

import com.zzp.pvms.common.constant.MqConstants;
import com.zzp.pvms.common.dto.MediaProcessMsg;
import com.zzp.pvms.media.domain.MonitorDirectory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class FileScannerService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final ExecutorService scanExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 支持的图片扩展名
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "arw", "cr3", "dng", "nef");
    // 支持的视频扩展名
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "avi", "mkv", "flv");

    public void scanDirectory(MonitorDirectory directory) {
        scanExecutor.submit(() -> {
            log.info("开始扫描目录: {}", directory.getDirPath());
            try {
                Path start = Paths.get(directory.getDirPath());
                Files.walkFileTree(start, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (isMediaFile(file)) {
                            sendToMq(file, directory.getId());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        log.error("访问文件失败: {}, 原因: {}", file, exc.getMessage());
                        return FileVisitResult.CONTINUE;
                    }
                });
                log.info("目录扫描完成: {}", directory.getDirPath());
            } catch (IOException e) {
                log.error("扫描目录异常: {}", directory.getDirPath(), e);
            }
        });
    }

    private boolean isMediaFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) return false;
        String extension = fileName.substring(lastDotIndex + 1);
        return IMAGE_EXTENSIONS.contains(extension) || VIDEO_EXTENSIONS.contains(extension);
    }

    private void sendToMq(Path file, Long directoryId) {
        MediaProcessMsg msg = MediaProcessMsg.builder()
                .filePath(file.toAbsolutePath().toString())
                .directoryId(directoryId)
                .taskType(1) // 1: 提取元数据
                .build();
        
        rabbitTemplate.convertAndSend(
                MqConstants.MEDIA_EXCHANGE,
                MqConstants.METADATA_ROUTING_KEY,
                msg
        );
        log.debug("已发送文件到MQ: {}", file);
    }
}
