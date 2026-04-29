package com.zzp.pvms.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.media.domain.MediaTagRelation;
import com.zzp.pvms.media.domain.Tag;
import com.zzp.pvms.media.mapper.MediaTagRelationMapper;
import com.zzp.pvms.media.mapper.TagMapper;
import com.zzp.pvms.media.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Autowired
    private MediaTagRelationMapper relationMapper;

    @Override
    public void addTagToMedia(Long mediaId, Long tagId) {
        // 检查关系是否已存在
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
    public void removeTagFromMedia(Long mediaId, Long tagId) {
        relationMapper.delete(new LambdaQueryWrapper<MediaTagRelation>()
                .eq(MediaTagRelation::getMediaId, mediaId)
                .eq(MediaTagRelation::getTagId, tagId));
    }

    @Override
    public List<Tag> getTagsByMediaId(Long mediaId) {
        // 先查出所有关联的 tagId
        List<MediaTagRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<MediaTagRelation>()
                .eq(MediaTagRelation::getMediaId, mediaId));
        
        if (relations.isEmpty()) return List.of();
        
        List<Long> tagIds = relations.stream().map(MediaTagRelation::getTagId).collect(Collectors.toList());
        return this.baseMapper.selectBatchIds(tagIds);
    }
}
