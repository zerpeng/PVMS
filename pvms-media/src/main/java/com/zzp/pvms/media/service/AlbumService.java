package com.zzp.pvms.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzp.pvms.media.domain.Album;

import java.util.List;

public interface AlbumService extends IService<Album> {
    List<Album> listActiveAlbums();

    Album createAlbum(Album album);

    Album updateAlbum(Long id, Album album);

    boolean removeAlbums(List<Long> ids);

    void addMediaToAlbum(Long albumId, Long mediaId);

    void removeMediaFromAlbum(Long albumId, Long mediaId);
}
