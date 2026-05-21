package com.zzp.pvms.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.common.exception.ApiException;
import com.zzp.pvms.media.domain.Album;
import com.zzp.pvms.media.domain.AuthContext;
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
    public List<Album> listActiveAlbums(AuthContext authContext) {
        LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<Album>()
                .orderByDesc(Album::getCreateTime)
                .orderByDesc(Album::getId);
        applyVisibility(wrapper, authContext);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Album createAlbum(Album album, AuthContext authContext) {
        assertWritable(authContext, "访客无新增相册权限");
        Album target = new Album();
        applyAlbumChanges(target, album, null, authContext);
        target.setCreateUsername(authContext.getUsername());
        target.setCreateTime(LocalDateTime.now());
        save(target);
        return getById(target.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Album updateAlbum(Long id, Album album, AuthContext authContext) {
        assertWritable(authContext, "访客无修改相册权限");
        Album existing = getVisibleAlbum(id, authContext);
        if (existing == null) {
            throw new ApiException("相册不存在或无权访问");
        }

        applyAlbumChanges(existing, album, id, authContext);
        updateById(existing);
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeAlbums(List<Long> ids, AuthContext authContext) {
        assertWritable(authContext, "访客无删除相册权限");
        List<Long> targetIds = ids == null ? Collections.emptyList() : ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (targetIds.isEmpty()) {
            throw new ApiException("请选择要移除的相册");
        }

        List<Long> visibleIds = listActiveAlbums(authContext).stream().map(Album::getId).toList();
        if (!authContext.isSuperAdmin() && !visibleIds.containsAll(targetIds)) {
            throw new ApiException("存在无权操作的相册");
        }

        relationMapper.delete(new LambdaQueryWrapper<MediaAlbumRelation>()
                .in(MediaAlbumRelation::getAlbumId, targetIds));
        return removeByIds(targetIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMediaToAlbum(Long albumId, Long mediaId, AuthContext authContext) {
        assertWritable(authContext, "访客无添加相册内容权限");
        if (getVisibleAlbum(albumId, authContext) == null) {
            throw new ApiException("相册不存在或无权访问");
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
    public void removeMediaFromAlbum(Long albumId, Long mediaId, AuthContext authContext) {
        assertWritable(authContext, "访客无移除相册内容权限");
        if (getVisibleAlbum(albumId, authContext) == null) {
            throw new ApiException("相册不存在或无权访问");
        }
        relationMapper.delete(new LambdaQueryWrapper<MediaAlbumRelation>()
                .eq(MediaAlbumRelation::getAlbumId, albumId)
                .eq(MediaAlbumRelation::getMediaId, mediaId));
    }

    private Album getVisibleAlbum(Long id, AuthContext authContext) {
        LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<Album>().eq(Album::getId, id);
        applyVisibility(wrapper, authContext);
        return getOne(wrapper.last("LIMIT 1"));
    }

    private void applyVisibility(LambdaQueryWrapper<Album> wrapper, AuthContext authContext) {
        if (authContext.isSuperAdmin()) {
            return;
        }
        List<Long> permittedIds = authContext.getAlbumIds();
        if (authContext.isVisitor()) {
            if (permittedIds.isEmpty()) {
                wrapper.eq(Album::getId, -1L);
            } else {
                wrapper.in(Album::getId, permittedIds);
            }
            return;
        }
        wrapper.and(scope -> {
            scope.eq(Album::getCreateUsername, authContext.getUsername());
            if (!permittedIds.isEmpty()) {
                scope.or().in(Album::getId, permittedIds);
            }
        });
    }

    private void applyAlbumChanges(Album target, Album source, Long currentId, AuthContext authContext) {
        String name = source == null ? null : normalizeText(source.getName());
        if (name == null) {
            throw new ApiException("相册名称不能为空");
        }

        LambdaQueryWrapper<Album> duplicateWrapper = new LambdaQueryWrapper<Album>()
                .eq(Album::getName, name)
                .ne(currentId != null, Album::getId, currentId);
        if (!authContext.isSuperAdmin()) {
            duplicateWrapper.eq(Album::getCreateUsername, authContext.getUsername());
        }
        if (count(duplicateWrapper) > 0) {
            throw new ApiException("相册名称已存在");
        }

        Integer albumType = source.getAlbumType() == null ? 1 : source.getAlbumType();
        target.setName(name);
        target.setDescription(normalizeText(source.getDescription()));
        target.setAlbumType(albumType);
        target.setQueryRule(albumType == 2 ? normalizeText(source.getQueryRule()) : null);
        target.setCoverMediaId(source.getCoverMediaId());
    }

    private void assertWritable(AuthContext authContext, String message) {
        if (!authContext.canWrite()) {
            throw new ApiException(message);
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
