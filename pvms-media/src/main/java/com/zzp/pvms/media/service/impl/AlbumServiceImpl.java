package com.zzp.pvms.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.common.exception.ApiException;
import com.zzp.pvms.media.domain.Album;
import com.zzp.pvms.media.domain.MediaAlbumRelation;
import com.zzp.pvms.media.mapper.AlbumMapper;
import com.zzp.pvms.media.mapper.MediaAlbumRelationMapper;
import com.zzp.pvms.media.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class AlbumServiceImpl extends ServiceImpl<AlbumMapper, Album> implements AlbumService {

    @Autowired
    private MediaAlbumRelationMapper relationMapper;

    @Override
    public List<Album> listActiveAlbums() {
        return list(new LambdaQueryWrapper<Album>()
                .orderByDesc(Album::getCreateTime)
                .orderByDesc(Album::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Album createAlbum(Album album) {
        Album target = new Album();
        applyAlbumChanges(target, album, null);
        target.setCreateTime(LocalDateTime.now());
        save(target);
        return getById(target.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Album updateAlbum(Long id, Album album) {
        Album existing = getById(id);
        if (existing == null) {
            throw new ApiException("相册不存在");
        }

        applyAlbumChanges(existing, album, id);
        updateById(existing);
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeAlbums(List<Long> ids) {
        List<Long> targetIds = ids == null ? Collections.emptyList() : ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (targetIds.isEmpty()) {
            throw new ApiException("请选择要移除的相册");
        }

        relationMapper.delete(new LambdaQueryWrapper<MediaAlbumRelation>()
                .in(MediaAlbumRelation::getAlbumId, targetIds));
        return removeByIds(targetIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMediaToAlbum(Long albumId, Long mediaId) {
        if (getById(albumId) == null) {
            throw new ApiException("相册不存在");
        }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMediaFromAlbum(Long albumId, Long mediaId) {
        relationMapper.delete(new LambdaQueryWrapper<MediaAlbumRelation>()
                .eq(MediaAlbumRelation::getAlbumId, albumId)
                .eq(MediaAlbumRelation::getMediaId, mediaId));
    }

    private void applyAlbumChanges(Album target, Album source, Long currentId) {
        String name = source == null ? null : normalizeText(source.getName());
        if (name == null) {
            throw new ApiException("相册名称不能为空");
        }

        long duplicateCount = count(new LambdaQueryWrapper<Album>()
                .eq(Album::getName, name)
                .ne(currentId != null, Album::getId, currentId));
        if (duplicateCount > 0) {
            throw new ApiException("相册名称已存在");
        }

        Integer albumType = source.getAlbumType() == null ? 1 : source.getAlbumType();
        target.setName(name);
        target.setDescription(normalizeText(source.getDescription()));
        target.setAlbumType(albumType);
        target.setQueryRule(albumType == 2 ? normalizeText(source.getQueryRule()) : null);
        target.setCoverMediaId(source.getCoverMediaId());
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
