package com.zzp.pvms.media.controller;

import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.domain.Album;
import com.zzp.pvms.media.domain.dto.IdsRequest;
import com.zzp.pvms.media.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/albums")
public class AlbumController {

    @Autowired
    private AlbumService albumService;

    @GetMapping
    public Result<List<Album>> list() {
        return Result.success(albumService.listActiveAlbums());
    }

    @PostMapping
    public Result<Album> create(@RequestBody Album album) {
        return Result.success(albumService.createAlbum(album));
    }

    @PutMapping("/{id}")
    public Result<Album> update(@PathVariable Long id, @RequestBody Album album) {
        return Result.success(albumService.updateAlbum(id, album));
    }

    @PostMapping("/{albumId}/add/{mediaId}")
    public Result<String> addMedia(@PathVariable Long albumId, @PathVariable Long mediaId) {
        albumService.addMediaToAlbum(albumId, mediaId);
        return Result.success("添加成功");
    }

    @DeleteMapping("/{albumId}/media/{mediaId}")
    public Result<String> removeMedia(@PathVariable Long albumId, @PathVariable Long mediaId) {
        albumService.removeMediaFromAlbum(albumId, mediaId);
        return Result.success("绉婚櫎鎴愬姛");
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(albumService.removeAlbums(List.of(id)));
    }

    @DeleteMapping
    public Result<Boolean> removeBatch(@RequestBody(required = false) IdsRequest request) {
        return Result.success(albumService.removeAlbums(request == null ? null : request.getIds()));
    }
}
