package com.zzp.pvms.media.controller;

import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.domain.Album;
import com.zzp.pvms.media.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/albums")
public class AlbumController {

    @Autowired
    private AlbumService albumService;

    @GetMapping
    public Result<List<Album>> list() {
        return Result.success(albumService.list());
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody Album album) {
        return Result.success(albumService.save(album));
    }

    @PostMapping("/{albumId}/add/{mediaId}")
    public Result<String> addMedia(@PathVariable Long albumId, @PathVariable Long mediaId) {
        albumService.addMediaToAlbum(albumId, mediaId);
        return Result.success("添加成功");
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(albumService.removeById(id));
    }
}
