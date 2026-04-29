package com.zzp.pvms.media.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 媒体与标签关联关系
 */
@Data
@TableName("media_tag_relation")
public class MediaTagRelation {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long mediaId;

    private Long tagId;

    /** 置信度 (AI打标用) */
    private BigDecimal confidence;
}
