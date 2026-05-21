package com.zzp.pvms.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.common.exception.ApiException;
import com.zzp.pvms.media.domain.MediaFavorite;
import com.zzp.pvms.media.mapper.MediaFileMapper;
import com.zzp.pvms.media.mapper.MediaFavoriteMapper;
import com.zzp.pvms.media.service.MediaFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class MediaFavoriteServiceImpl extends ServiceImpl<MediaFavoriteMapper, MediaFavorite> implements MediaFavoriteService {

    @Autowired
    private MediaFileMapper mediaFileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addFavorite(String username, Long mediaId) {
        if (mediaFileMapper.selectActiveById(mediaId) == null) {
            throw new ApiException("媒体文件不存在");
        }

        String owner = resolveUsername(username);
        Long count = count(new LambdaQueryWrapper<MediaFavorite>()
                .eq(MediaFavorite::getUsername, owner)
                .eq(MediaFavorite::getMediaId, mediaId));
        if (count > 0) {
            return true;
        }

        MediaFavorite favorite = new MediaFavorite();
        favorite.setUsername(owner);
        favorite.setMediaId(mediaId);
        favorite.setCreateTime(LocalDateTime.now());
        return save(favorite);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeFavorite(String username, Long mediaId) {
        return remove(new LambdaQueryWrapper<MediaFavorite>()
                .eq(MediaFavorite::getUsername, resolveUsername(username))
                .eq(MediaFavorite::getMediaId, mediaId));
    }

    private String resolveUsername(String username) {
        return StringUtils.hasText(username) ? username : "anonymous";
    }
}
