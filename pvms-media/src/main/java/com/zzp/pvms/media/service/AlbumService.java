package com.zzp.pvms.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzp.pvms.media.domain.Album;
import com.zzp.pvms.media.domain.AuthContext;

import java.util.List;

public interface AlbumService extends IService<Album> {
    List<Album> listActiveAlbums(AuthContext authContext);

    Album createAlbum(Album album, AuthContext authContext);

    Album updateAlbum(Long id, Album album, AuthContext authContext);

    boolean removeAlbums(List<Long> ids, AuthContext authContext);

    void addMediaToAlbum(Long albumId, Long mediaId, AuthContext authContext);

    void removeMediaFromAlbum(Long albumId, Long mediaId, AuthContext authContext);
}
