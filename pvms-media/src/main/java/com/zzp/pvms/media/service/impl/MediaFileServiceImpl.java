package com.zzp.pvms.media.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.zzp.pvms.common.constant.MqConstants;
import com.zzp.pvms.common.dto.MediaProcessMsg;
import com.zzp.pvms.common.exception.ApiException;
import com.zzp.pvms.media.domain.AuthContext;
import com.zzp.pvms.media.domain.Album;
import com.zzp.pvms.media.domain.MediaAlbumRelation;
import com.zzp.pvms.media.domain.MediaFile;
import com.zzp.pvms.media.domain.MonitorDirectory;
import com.zzp.pvms.media.domain.MediaTagRelation;
import com.zzp.pvms.media.domain.Tag;
import com.zzp.pvms.media.mapper.AlbumMapper;
import com.zzp.pvms.media.mapper.MediaAlbumRelationMapper;
import com.zzp.pvms.media.mapper.MediaFileMapper;
import com.zzp.pvms.media.mapper.MonitorDirectoryMapper;
import com.zzp.pvms.media.mapper.MediaTagRelationMapper;
import com.zzp.pvms.media.mapper.TagMapper;
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

    @Autowired
    private MonitorDirectoryMapper monitorDirectoryMapper;

    @Autowired
    private AlbumMapper albumMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private MediaAlbumRelationMapper mediaAlbumRelationMapper;

    @Autowired
    private MediaTagRelationMapper mediaTagRelationMapper;

    @Override
    public Page<MediaFile> queryPage(int current, int size, Integer mediaType, Long albumId, Long tagId, Boolean favoriteOnly, AuthContext authContext) {
        if (albumId != null && !canAccessAlbum(albumId, authContext)) {
            throw new ApiException("无权访问该相册");
        }
        if (tagId != null && !canAccessTag(tagId, authContext)) {
            throw new ApiException("无权访问该标签");
        }
        Page<MediaFile> page = new Page<>(current, size);
        return (Page<MediaFile>) baseMapper.selectActivePage(
                page,
                mediaType,
                albumId,
                tagId,
                favoriteOnly,
                resolveUsername(authContext.getUsername()),
                authContext.isSuperAdmin(),
                resolveDirectoryIds(authContext),
                authContext.getAlbumIds());
    }

    @Override
    public MediaFile getActiveById(Long id, AuthContext authContext) {
        MediaFile mediaFile = baseMapper.selectActiveById(id);
        if (mediaFile == null || !canAccessMedia(mediaFile, authContext)) {
            return null;
        }
        return mediaFile;
    }

    @Override
    public List<Map<String, Object>> getTimeLine(AuthContext authContext) {
        if (!authContext.isSuperAdmin() && resolveDirectoryIds(authContext).isEmpty()) {
            return List.of();
        }
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

        try {
            MediaFile mediaFile = buildMediaFile(file, filePath, msg.getDirectoryId());
            MediaFile existingFile = baseMapper.selectAnyByFilePath(filePath);

            if (existingFile != null && Integer.valueOf(0).equals(existingFile.getDelFlag())) {
                log.debug("File already exists in database, skip: {}", filePath);
                return;
            }

            if (existingFile != null) {
                mediaFile.setId(existingFile.getId());
                baseMapper.restoreById(mediaFile);
                log.info("Metadata restored and updated: {}", filePath);
                sendThumbnailMsg(mediaFile);
                return;
            }

            save(mediaFile);
            log.info("Metadata extracted and saved: {}", filePath);

            sendThumbnailMsg(mediaFile);
        } catch (Exception e) {
            log.error("Process metadata failed: {}", filePath, e);
        }
    }

    private MediaFile buildMediaFile(File file, String filePath, Long directoryId) {
        String fileHash = DigestUtil.md5Hex(file);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileName(file.getName());
        mediaFile.setFilePath(filePath);
        mediaFile.setFileHash(fileHash);
        mediaFile.setFileSize(file.length());
        mediaFile.setDirectoryId(directoryId);
        mediaFile.setFileFormat(getFileExtension(file.getName()));
        mediaFile.setMediaType(determineMediaType(mediaFile.getFileFormat()));

        extractExif(file, mediaFile);
        return mediaFile;
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

    private List<Long> resolveDirectoryIds(AuthContext authContext) {
        if (authContext.isSuperAdmin()) {
            return List.of();
        }
        List<Long> permittedIds = new java.util.ArrayList<>(authContext.getDirectoryIds());
        if (!authContext.isVisitor()) {
            List<MonitorDirectory> ownedDirectories = monitorDirectoryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MonitorDirectory>()
                            .eq(MonitorDirectory::getCreateUsername, authContext.getUsername()));
            ownedDirectories.stream()
                    .map(MonitorDirectory::getId)
                    .filter(id -> !permittedIds.contains(id))
                    .forEach(permittedIds::add);
        }
        return permittedIds;
    }

    private boolean canAccessAlbum(Long albumId, AuthContext authContext) {
        if (authContext.isSuperAdmin() || authContext.getAlbumIds().contains(albumId)) {
            return true;
        }
        if (authContext.isVisitor()) {
            return false;
        }
        Album album = albumMapper.selectById(albumId);
        return album != null && authContext.getUsername().equals(album.getCreateUsername());
    }

    private boolean canAccessTag(Long tagId, AuthContext authContext) {
        if (authContext.isSuperAdmin() || authContext.getTagIds().contains(tagId)) {
            return true;
        }
        if (authContext.isVisitor()) {
            return false;
        }
        Tag tag = tagMapper.selectById(tagId);
        return tag != null && authContext.getUsername().equals(tag.getCreateUsername());
    }

    private boolean canAccessMedia(MediaFile mediaFile, AuthContext authContext) {
        if (authContext.isSuperAdmin()) {
            return true;
        }
        if (resolveDirectoryIds(authContext).contains(mediaFile.getDirectoryId())) {
            return true;
        }
        if (!authContext.getAlbumIds().isEmpty()) {
            Long albumCount = mediaAlbumRelationMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MediaAlbumRelation>()
                            .eq(MediaAlbumRelation::getMediaId, mediaFile.getId())
                            .in(MediaAlbumRelation::getAlbumId, authContext.getAlbumIds()));
            if (albumCount > 0) {
                return true;
            }
        }
        if (!authContext.getTagIds().isEmpty()) {
            Long tagCount = mediaTagRelationMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MediaTagRelation>()
                            .eq(MediaTagRelation::getMediaId, mediaFile.getId())
                            .in(MediaTagRelation::getTagId, authContext.getTagIds()));
            return tagCount > 0;
        }
        return false;
    }
}
