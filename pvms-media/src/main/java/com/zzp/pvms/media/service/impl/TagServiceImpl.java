package com.zzp.pvms.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.common.exception.ApiException;
import com.zzp.pvms.media.domain.AuthContext;
import com.zzp.pvms.media.domain.MediaTagRelation;
import com.zzp.pvms.media.domain.Tag;
import com.zzp.pvms.media.mapper.MediaTagRelationMapper;
import com.zzp.pvms.media.mapper.TagMapper;
import com.zzp.pvms.media.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Autowired
    private MediaTagRelationMapper relationMapper;

    @Override
    public List<Tag> listActiveTags(AuthContext authContext) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<Tag>()
                .orderByAsc(Tag::getTagType)
                .orderByAsc(Tag::getName)
                .orderByDesc(Tag::getId);
        applyVisibility(wrapper, authContext);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tag createTag(Tag tag, AuthContext authContext) {
        assertWritable(authContext, "访客无新增标签权限");
        Tag target = new Tag();
        applyTagChanges(target, tag, null, authContext);
        target.setCreateUsername(authContext.getUsername());

        Tag existing = baseMapper.selectAnyByNameAndType(target.getName(), target.getTagType());
        if (existing != null) {
            if (Integer.valueOf(0).equals(existing.getDelFlag())) {
                throw new ApiException("标签名称已存在");
            }

            existing.setParentId(target.getParentId());
            existing.setCategory(target.getCategory());
            existing.setCreateUsername(authContext.getUsername());
            baseMapper.restoreById(existing);
            return getById(existing.getId());
        }

        save(target);
        return getById(target.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tag updateTag(Long id, Tag tag, AuthContext authContext) {
        assertWritable(authContext, "访客无修改标签权限");
        Tag existing = getVisibleTag(id, authContext);
        if (existing == null) {
            throw new ApiException("标签不存在或无权访问");
        }

        Tag target = new Tag();
        applyTagChanges(target, tag, id, authContext);

        Tag duplicate = baseMapper.selectAnyByNameAndType(target.getName(), target.getTagType());
        if (duplicate != null && !duplicate.getId().equals(id)) {
            if (Integer.valueOf(0).equals(duplicate.getDelFlag())) {
                throw new ApiException("标签名称已存在");
            }
            throw new ApiException("存在同名已删除标签，请更换标签名称");
        }

        existing.setName(target.getName());
        existing.setParentId(target.getParentId());
        existing.setTagType(target.getTagType());
        existing.setCategory(target.getCategory());
        updateById(existing);
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTags(List<Long> ids, AuthContext authContext) {
        assertWritable(authContext, "访客无删除标签权限");
        List<Long> targetIds = ids == null ? Collections.emptyList() : ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (targetIds.isEmpty()) {
            throw new ApiException("请选择要移除的标签");
        }
        List<Long> visibleIds = listActiveTags(authContext).stream().map(Tag::getId).toList();
        if (!authContext.isSuperAdmin() && !visibleIds.containsAll(targetIds)) {
            throw new ApiException("存在无权操作的标签");
        }

        relationMapper.delete(new LambdaQueryWrapper<MediaTagRelation>()
                .in(MediaTagRelation::getTagId, targetIds));
        return removeByIds(targetIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTagToMedia(Long mediaId, Long tagId, AuthContext authContext) {
        assertWritable(authContext, "访客无绑定标签权限");
        if (getVisibleTag(tagId, authContext) == null) {
            throw new ApiException("标签不存在或无权访问");
        }

        Long count = relationMapper.selectCount(new LambdaQueryWrapper<MediaTagRelation>()
                .eq(MediaTagRelation::getMediaId, mediaId)
                .eq(MediaTagRelation::getTagId, tagId));

        if (count == 0) {
            MediaTagRelation relation = new MediaTagRelation();
            relation.setMediaId(mediaId);
            relation.setTagId(tagId);
            relationMapper.insert(relation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTagFromMedia(Long mediaId, Long tagId, AuthContext authContext) {
        assertWritable(authContext, "访客无解绑标签权限");
        if (getVisibleTag(tagId, authContext) == null) {
            throw new ApiException("标签不存在或无权访问");
        }
        relationMapper.delete(new LambdaQueryWrapper<MediaTagRelation>()
                .eq(MediaTagRelation::getMediaId, mediaId)
                .eq(MediaTagRelation::getTagId, tagId));
    }

    @Override
    public List<Tag> getTagsByMediaId(Long mediaId, AuthContext authContext) {
        List<MediaTagRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<MediaTagRelation>()
                .eq(MediaTagRelation::getMediaId, mediaId));

        if (relations.isEmpty()) {
            return List.of();
        }

        List<Long> tagIds = relations.stream().map(MediaTagRelation::getTagId).collect(Collectors.toList());
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<Tag>().in(Tag::getId, tagIds);
        applyVisibility(wrapper, authContext);
        return list(wrapper);
    }

    private Tag getVisibleTag(Long id, AuthContext authContext) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<Tag>().eq(Tag::getId, id);
        applyVisibility(wrapper, authContext);
        return getOne(wrapper.last("LIMIT 1"));
    }

    private void applyVisibility(LambdaQueryWrapper<Tag> wrapper, AuthContext authContext) {
        if (authContext.isSuperAdmin()) {
            return;
        }
        List<Long> permittedIds = authContext.getTagIds();
        if (authContext.isVisitor()) {
            if (permittedIds.isEmpty()) {
                wrapper.eq(Tag::getId, -1L);
            } else {
                wrapper.in(Tag::getId, permittedIds);
            }
            return;
        }
        wrapper.and(scope -> {
            scope.eq(Tag::getCreateUsername, authContext.getUsername());
            if (!permittedIds.isEmpty()) {
                scope.or().in(Tag::getId, permittedIds);
            }
        });
    }

    private void applyTagChanges(Tag target, Tag source, Long currentId, AuthContext authContext) {
        String name = source == null ? null : normalizeText(source.getName());
        if (name == null) {
            throw new ApiException("标签名称不能为空");
        }

        Integer tagType = source.getTagType() == null ? 1 : source.getTagType();
        Long parentId = source.getParentId() == null ? 0L : source.getParentId();
        if (currentId != null && currentId.equals(parentId)) {
            throw new ApiException("父标签不能选择自身");
        }

        target.setName(name);
        target.setTagType(tagType);
        target.setParentId(parentId);
        target.setCategory(normalizeText(source.getCategory()));
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
