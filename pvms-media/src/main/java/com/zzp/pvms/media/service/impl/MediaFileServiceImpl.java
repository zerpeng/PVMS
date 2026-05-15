package com.zzp.pvms.media.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.zzp.pvms.common.constant.MqConstants;
import com.zzp.pvms.common.dto.MediaProcessMsg;
import com.zzp.pvms.media.domain.MediaFile;
import com.zzp.pvms.media.mapper.MediaFileMapper;
import com.zzp.pvms.media.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MediaFileServiceImpl extends ServiceImpl<MediaFileMapper, MediaFile> implements MediaFileService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Page<MediaFile> queryPage(int current, int size, Integer mediaType, Long albumId, Long tagId, Boolean favoriteOnly, String username) {
        Page<MediaFile> page = new Page<>(current, size);
        return (Page<MediaFile>) baseMapper.selectActivePage(page, mediaType, albumId, tagId, favoriteOnly, resolveUsername(username));
    }

    @Override
    public MediaFile getActiveById(Long id) {
        return baseMapper.selectActiveById(id);
    }

    @Override
    public List<Map<String, Object>> getTimeLine() {
        return baseMapper.selectActiveTimeline();
    }

    @Override
    public void processMetadata(MediaProcessMsg msg) {
        String filePath = msg.getFilePath();
        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("File does not exist, skip processing: {}", filePath);
            return;
        }

        MediaFile existingFile = getOne(new LambdaQueryWrapper<MediaFile>()
                .eq(MediaFile::getFilePath, filePath)
                .eq(MediaFile::getDelFlag, 0));
        if (existingFile != null) {
            log.debug("File already exists in database, skip: {}", filePath);
            return;
        }

        try {
            String fileHash = DigestUtil.md5Hex(file);

            MediaFile mediaFile = new MediaFile();
            mediaFile.setFileName(file.getName());
            mediaFile.setFilePath(filePath);
            mediaFile.setFileHash(fileHash);
            mediaFile.setFileSize(file.length());
            mediaFile.setDirectoryId(msg.getDirectoryId());
            mediaFile.setFileFormat(getFileExtension(file.getName()));
            mediaFile.setMediaType(determineMediaType(mediaFile.getFileFormat()));

            extractExif(file, mediaFile);

            save(mediaFile);
            log.info("Metadata extracted and saved: {}", filePath);

            sendThumbnailMsg(mediaFile);
        } catch (Exception e) {
            log.error("Process metadata failed: {}", filePath, e);
        }
    }

    private void sendThumbnailMsg(MediaFile mediaFile) {
        MediaProcessMsg msg = MediaProcessMsg.builder()
                .mediaId(mediaFile.getId())
                .filePath(mediaFile.getFilePath())
                .taskType(2)
                .build();
        rabbitTemplate.convertAndSend(MqConstants.MEDIA_EXCHANGE, MqConstants.THUMBNAIL_ROUTING_KEY, msg);
    }

    private void extractExif(File file, MediaFile mediaFile) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            ExifIFD0Directory ifd0Dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0Dir != null) {
                mediaFile.setCameraMake(ifd0Dir.getString(ExifIFD0Directory.TAG_MAKE));
                mediaFile.setCameraModel(ifd0Dir.getString(ExifIFD0Directory.TAG_MODEL));
            }

            ExifSubIFDDirectory subIfdDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIfdDir != null) {
                mediaFile.setFNumber(subIfdDir.getString(ExifSubIFDDirectory.TAG_FNUMBER));
                mediaFile.setExposureTime(subIfdDir.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
                mediaFile.setIso(subIfdDir.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
                mediaFile.setFocalLength(subIfdDir.getInteger(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
                mediaFile.setWidth(subIfdDir.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH));
                mediaFile.setHeight(subIfdDir.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT));

                Date date = subIfdDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    mediaFile.setShotTime(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
                }
            }

            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null && gpsDir.getGeoLocation() != null) {
                mediaFile.setGpsLatitude(new java.math.BigDecimal(String.valueOf(gpsDir.getGeoLocation().getLatitude())));
                mediaFile.setGpsLongitude(new java.math.BigDecimal(String.valueOf(gpsDir.getGeoLocation().getLongitude())));
            }
        } catch (Exception e) {
            log.warn("EXIF parse failed: {}", file.getName());
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    private Integer determineMediaType(String extension) {
        return "mp4".equals(extension) || "mov".equals(extension) ? 2 : 1;
    }

    private String resolveUsername(String username) {
        return StringUtils.hasText(username) ? username : "anonymous";
    }
}
