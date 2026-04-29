package com.zzp.pvms.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.media.domain.Album;
import com.zzp.pvms.media.domain.MediaAlbumRelation;
import com.zzp.pvms.media.mapper.AlbumMapper;
import com.zzp.pvms.media.mapper.MediaAlbumRelationMapper;
import com.zzp.pvms.media.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AlbumServiceImpl extends ServiceImpl<AlbumMapper, Album> implements AlbumService {

    @Autowired
    private MediaAlbumRelationMapper relationMapper;

    @Override
    public void addMediaToAlbum(Long albumId, Long mediaId) {
        Long count = relationMapper.selectCount(new LambdaQueryWrapper<MediaAlbumRelation>()
                .eq(MediaAlbumRelation::getAlbumId, albumId)
                .eq(MediaAlbumRelation::getMediaId, mediaId));
        
        if (count == 0) {
            MediaAlbumRelation relation = new MediaAlbumRelation();
            relation.setAlbumId(albumId);
            relation.setMediaId(mediaId);
            relationMapper.insert(relation);
        }
    }
}
