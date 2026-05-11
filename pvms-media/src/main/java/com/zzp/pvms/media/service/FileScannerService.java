package com.zzp.pvms.media.service;

import com.zzp.pvms.common.constant.MqConstants;
import com.zzp.pvms.common.dto.MediaProcessMsg;
import com.zzp.pvms.media.domain.MonitorDirectory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class FileScannerService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MonitorDirectoryService monitorDirectoryService;

    private final ExecutorService scanExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "arw", "cr3", "dng", "nef");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "avi", "mkv", "flv");

    public void scanDirectory(MonitorDirectory directory) {
        scanExecutor.submit(() -> {
            log.info("Start scanning directory: {}", directory.getDirPath());
            updateScanState(directory.getId(), 1, null);

            try {
                Path start = Paths.get(directory.getDirPath());
                int maxDepth = Integer.valueOf(1).equals(directory.getIsRecursive()) ? Integer.MAX_VALUE : 1;

                Files.walkFileTree(start, Set.of(), maxDepth, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (isMediaFile(file)) {
                            sendToMq(file, directory.getId());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        log.error("Visit file failed: {}, reason: {}", file, exc.getMessage());
                        return FileVisitResult.CONTINUE;
                    }
                });

                log.info("Directory scan completed: {}", directory.getDirPath());
                updateScanState(directory.getId(), 0, LocalDateTime.now());
            } catch (IOException e) {
                log.error("Directory scan failed: {}", directory.getDirPath(), e);
                updateScanState(directory.getId(), 0, null);
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
                .taskType(1)
                .build();

        rabbitTemplate.convertAndSend(
                MqConstants.MEDIA_EXCHANGE,
                MqConstants.METADATA_ROUTING_KEY,
                msg
        );
        log.debug("Sent file to MQ: {}", file);
    }

    private void updateScanState(Long directoryId, Integer status, LocalDateTime lastScanTime) {
        MonitorDirectory update = new MonitorDirectory();
        update.setId(directoryId);
        update.setScanStatus(status);
        if (lastScanTime != null) {
            update.setLastScanTime(lastScanTime);
        }
        monitorDirectoryService.updateById(update);
    }
}
