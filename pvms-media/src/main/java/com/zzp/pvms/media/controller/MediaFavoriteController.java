package com.zzp.pvms.media.controller;

import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.service.MediaFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/favorites")
public class MediaFavoriteController {

    @Autowired
    private MediaFavoriteService mediaFavoriteService;

    @PostMapping("/{mediaId}")
    public Result<Boolean> addFavorite(
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @PathVariable Long mediaId) {
        return Result.success(mediaFavoriteService.addFavorite(username, mediaId));
    }

    @DeleteMapping("/{mediaId}")
    public Result<Boolean> removeFavorite(
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @PathVariable Long mediaId) {
        return Result.success(mediaFavoriteService.removeFavorite(username, mediaId));
    }
}
