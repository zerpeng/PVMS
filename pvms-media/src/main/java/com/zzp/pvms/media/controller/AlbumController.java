package com.zzp.pvms.media.controller;

import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.domain.Album;
import com.zzp.pvms.media.domain.AuthContext;
import com.zzp.pvms.media.domain.dto.IdsRequest;
import com.zzp.pvms.media.service.AlbumService;
import jakarta.servlet.http.HttpServletRequest;
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
    public Result<List<Album>> list(HttpServletRequest request) {
        return Result.success(albumService.listActiveAlbums(auth(request)));
    }

    @PostMapping
    public Result<Album> create(@RequestBody Album album, HttpServletRequest request) {
        return Result.success(albumService.createAlbum(album, auth(request)));
    }

    @PutMapping("/{id}")
    public Result<Album> update(@PathVariable Long id, @RequestBody Album album, HttpServletRequest request) {
        return Result.success(albumService.updateAlbum(id, album, auth(request)));
    }

    @PostMapping("/{albumId}/add/{mediaId}")
    public Result<String> addMedia(@PathVariable Long albumId, @PathVariable Long mediaId, HttpServletRequest request) {
        albumService.addMediaToAlbum(albumId, mediaId, auth(request));
        return Result.success("添加成功");
    }

    @DeleteMapping("/{albumId}/media/{mediaId}")
    public Result<String> removeMedia(@PathVariable Long albumId, @PathVariable Long mediaId, HttpServletRequest request) {
        albumService.removeMediaFromAlbum(albumId, mediaId, auth(request));
        return Result.success("移除成功");
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(albumService.removeAlbums(List.of(id), auth(request)));
    }

    @DeleteMapping
    public Result<Boolean> removeBatch(@RequestBody(required = false) IdsRequest idsRequest, HttpServletRequest request) {
        return Result.success(albumService.removeAlbums(idsRequest == null ? null : idsRequest.getIds(), auth(request)));
    }

    private AuthContext auth(HttpServletRequest request) {
        return AuthContext.of(
                request.getHeader("X-User-Name"),
                request.getHeader("X-User-Role"),
                request.getHeader("X-Super-Admin"),
                request.getHeader("X-Visitor"),
                request.getHeader("X-Directory-Ids"),
                request.getHeader("X-Album-Ids"),
                request.getHeader("X-Tag-Ids"));
    }
}
