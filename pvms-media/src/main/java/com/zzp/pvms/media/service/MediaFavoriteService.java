package com.zzp.pvms.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzp.pvms.media.domain.MediaFavorite;

public interface MediaFavoriteService extends IService<MediaFavorite> {
    boolean addFavorite(String username, Long mediaId);

    boolean removeFavorite(String username, Long mediaId);
}
