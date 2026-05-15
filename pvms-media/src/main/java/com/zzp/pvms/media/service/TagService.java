package com.zzp.pvms.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzp.pvms.media.domain.Tag;

import java.util.List;

public interface TagService extends IService<Tag> {
    List<Tag> listActiveTags();

    Tag createTag(Tag tag);

    Tag updateTag(Long id, Tag tag);

    boolean removeTags(List<Long> ids);

    void addTagToMedia(Long mediaId, Long tagId);

    void removeTagFromMedia(Long mediaId, Long tagId);

    List<Tag> getTagsByMediaId(Long mediaId);
}
