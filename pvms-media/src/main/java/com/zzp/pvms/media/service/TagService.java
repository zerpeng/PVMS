package com.zzp.pvms.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzp.pvms.media.domain.AuthContext;
import com.zzp.pvms.media.domain.Tag;

import java.util.List;

public interface TagService extends IService<Tag> {
    List<Tag> listActiveTags(AuthContext authContext);

    Tag createTag(Tag tag, AuthContext authContext);

    Tag updateTag(Long id, Tag tag, AuthContext authContext);

    boolean removeTags(List<Long> ids, AuthContext authContext);

    void addTagToMedia(Long mediaId, Long tagId, AuthContext authContext);

    void removeTagFromMedia(Long mediaId, Long tagId, AuthContext authContext);

    List<Tag> getTagsByMediaId(Long mediaId, AuthContext authContext);
}
