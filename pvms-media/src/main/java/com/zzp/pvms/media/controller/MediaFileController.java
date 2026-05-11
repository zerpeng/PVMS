package com.zzp.pvms.media.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.domain.MediaFile;
import com.zzp.pvms.media.service.MediaFileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false) Integer mediaType) {
        return Result.success(mediaFileService.queryPage(current, size, mediaType));
    }

    @GetMapping("/timeline")
    public Result<List<Map<String, Object>>> timeline() {
        return Result.success(mediaFileService.getTimeLine());
    }

    @GetMapping("/{id}")
    public Result<MediaFile> detail(@PathVariable Long id) {
        return Result.success(mediaFileService.getById(id));
    }

    /**
     * 根据媒体 id 读取本地原始文件，前端通过鉴权请求获取 Blob 后再预览图片或视频。
     */
    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(@PathVariable Long id) {
        MediaFile mediaFile = mediaFileService.getById(id);
        if (mediaFile == null || mediaFile.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        File sourceFile = new File(mediaFile.getFilePath());
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            return ResponseEntity.notFound().build();
        }

        // 根据文件类型设置响应类型，让浏览器能正确渲染图片或视频。
        MediaType mediaType = resolveMediaType(mediaFile);
        Resource resource = new FileSystemResource(sourceFile);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(sourceFile.length())
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        // 仅为逻辑删除
        return Result.success(mediaFileService.removeById(id));
    }

    /**
     * 将数据库中的媒体类型和文件格式转换为 HTTP Content-Type。
     */
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
}
