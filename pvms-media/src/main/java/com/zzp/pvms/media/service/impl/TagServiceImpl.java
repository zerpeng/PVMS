package com.zzp.pvms.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.common.exception.ApiException;
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
    public List<Tag> listActiveTags() {
        return list(new LambdaQueryWrapper<Tag>()
                .orderByAsc(Tag::getTagType)
                .orderByAsc(Tag::getName)
                .orderByDesc(Tag::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tag createTag(Tag tag) {
        Tag target = new Tag();
        applyTagChanges(target, tag, null);

        Tag existing = baseMapper.selectAnyByNameAndType(target.getName(), target.getTagType());
        if (existing != null) {
            if (Integer.valueOf(0).equals(existing.getDelFlag())) {
                throw new ApiException("标签名称已存在");
            }

            existing.setParentId(target.getParentId());
            existing.setCategory(target.getCategory());
            baseMapper.restoreById(existing);
            return getById(existing.getId());
        }

        save(target);
        return getById(target.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tag updateTag(Long id, Tag tag) {
        Tag existing = getById(id);
        if (existing == null) {
            throw new ApiException("标签不存在");
        }

        Tag target = new Tag();
        applyTagChanges(target, tag, id);

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
    public boolean removeTags(List<Long> ids) {
        List<Long> targetIds = ids == null ? Collections.emptyList() : ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (targetIds.isEmpty()) {
            throw new ApiException("请选择要移除的标签");
        }

        relationMapper.delete(new LambdaQueryWrapper<MediaTagRelation>()
                .in(MediaTagRelation::getTagId, targetIds));
        return removeByIds(targetIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTagToMedia(Long mediaId, Long tagId) {
        if (getById(tagId) == null) {
            throw new ApiException("标签不存在");
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
    public void removeTagFromMedia(Long mediaId, Long tagId) {
        relationMapper.delete(new LambdaQueryWrapper<MediaTagRelation>()
                .eq(MediaTagRelation::getMediaId, mediaId)
                .eq(MediaTagRelation::getTagId, tagId));
    }

    @Override
    public List<Tag> getTagsByMediaId(Long mediaId) {
        List<MediaTagRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<MediaTagRelation>()
                .eq(MediaTagRelation::getMediaId, mediaId));

        if (relations.isEmpty()) {
            return List.of();
        }

        List<Long> tagIds = relations.stream().map(MediaTagRelation::getTagId).collect(Collectors.toList());
        return baseMapper.selectBatchIds(tagIds);
    }

    private void applyTagChanges(Tag target, Tag source, Long currentId) {
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

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
