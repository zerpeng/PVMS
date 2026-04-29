package com.zzp.pvms.media.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.domain.MediaFile;
import com.zzp.pvms.media.service.MediaFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        // 仅为逻辑删除
        return Result.success(mediaFileService.removeById(id));
    }
}
