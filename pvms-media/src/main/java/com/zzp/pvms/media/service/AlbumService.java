package com.zzp.pvms.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzp.pvms.media.domain.Album;

public interface AlbumService extends IService<Album> {
    /**
     * 向相册添加媒体文件
     */
    void addMediaToAlbum(Long albumId, Long mediaId);
}
