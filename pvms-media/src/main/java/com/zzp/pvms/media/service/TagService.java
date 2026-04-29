package com.zzp.pvms.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzp.pvms.media.domain.Tag;
import java.util.List;

public interface TagService extends IService<Tag> {
    /**
     * 为媒体文件关联标签
     */
    void addTagToMedia(Long mediaId, Long tagId);

    /**
     * 移除媒体文件的标签
     */
    void removeTagFromMedia(Long mediaId, Long tagId);

    /**
     * 获取媒体文件的所有标签
     */
    List<Tag> getTagsByMediaId(Long mediaId);
}
