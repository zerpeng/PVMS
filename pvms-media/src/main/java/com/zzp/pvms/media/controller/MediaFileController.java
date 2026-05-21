package com.zzp.pvms.media.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.domain.AuthContext;
import com.zzp.pvms.media.domain.MediaFile;
import com.zzp.pvms.media.service.MediaFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class MediaFileController {

    @Autowired
    private MediaFileService mediaFileService;

    @GetMapping
    public Result<Page<MediaFile>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer mediaType,
            @RequestParam(required = false) Long albumId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Boolean favoriteOnly,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Role", required = false) String roleKey,
            @RequestHeader(value = "X-Super-Admin", required = false) String superAdmin,
            @RequestHeader(value = "X-Visitor", required = false) String visitor,
            @RequestHeader(value = "X-Directory-Ids", required = false) String directoryIds,
            @RequestHeader(value = "X-Album-Ids", required = false) String albumIds,
            @RequestHeader(value = "X-Tag-Ids", required = false) String tagIds) {
        return Result.success(mediaFileService.queryPage(current, size, mediaType, albumId, tagId, favoriteOnly, auth(username, roleKey, superAdmin, visitor, directoryIds, albumIds, tagIds)));
    }

    @GetMapping("/timeline")
    public Result<List<Map<String, Object>>> timeline(
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Role", required = false) String roleKey,
            @RequestHeader(value = "X-Super-Admin", required = false) String superAdmin,
            @RequestHeader(value = "X-Visitor", required = false) String visitor,
            @RequestHeader(value = "X-Directory-Ids", required = false) String directoryIds,
            @RequestHeader(value = "X-Album-Ids", required = false) String albumIds,
            @RequestHeader(value = "X-Tag-Ids", required = false) String tagIds) {
        return Result.success(mediaFileService.getTimeLine(auth(username, roleKey, superAdmin, visitor, directoryIds, albumIds, tagIds)));
    }

    @GetMapping("/{id}")
    public Result<MediaFile> detail(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Role", required = false) String roleKey,
            @RequestHeader(value = "X-Super-Admin", required = false) String superAdmin,
            @RequestHeader(value = "X-Visitor", required = false) String visitor,
            @RequestHeader(value = "X-Directory-Ids", required = false) String directoryIds,
            @RequestHeader(value = "X-Album-Ids", required = false) String albumIds,
            @RequestHeader(value = "X-Tag-Ids", required = false) String tagIds) {
        return Result.success(mediaFileService.getActiveById(id, auth(username, roleKey, superAdmin, visitor, directoryIds, albumIds, tagIds)));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Role", required = false) String roleKey,
            @RequestHeader(value = "X-Super-Admin", required = false) String superAdmin,
            @RequestHeader(value = "X-Visitor", required = false) String visitor,
            @RequestHeader(value = "X-Directory-Ids", required = false) String directoryIds,
            @RequestHeader(value = "X-Album-Ids", required = false) String albumIds,
            @RequestHeader(value = "X-Tag-Ids", required = false) String tagIds) {
        MediaFile mediaFile = mediaFileService.getActiveById(id, auth(username, roleKey, superAdmin, visitor, directoryIds, albumIds, tagIds));
        if (mediaFile == null || mediaFile.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        File sourceFile = new File(mediaFile.getFilePath());
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = resolveMediaType(mediaFile);
        Resource resource = new FileSystemResource(sourceFile);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(sourceFile.length())
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(mediaFileService.removeById(id));
    }

    private MediaType resolveMediaType(MediaFile mediaFile) {
        String format = mediaFile.getFileFormat() == null ? "" : mediaFile.getFileFormat().toLowerCase();
        if ("png".equals(format)) {
            return MediaType.IMAGE_PNG;
        }
        if ("webp".equals(format)) {
            return MediaType.parseMediaType("image/webp");
        }
        if ("gif".equals(format)) {
            return MediaType.IMAGE_GIF;
        }
        if (Integer.valueOf(2).equals(mediaFile.getMediaType())) {
            if (format.isBlank()) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }
            return MediaType.parseMediaType("video/" + ("mov".equals(format) ? "quicktime" : format));
        }
        return MediaType.IMAGE_JPEG;
    }

    private AuthContext auth(String username, String roleKey, String superAdmin, String visitor, String directoryIds, String albumIds, String tagIds) {
        return AuthContext.of(username, roleKey, superAdmin, visitor, directoryIds, albumIds, tagIds);
    }
}
