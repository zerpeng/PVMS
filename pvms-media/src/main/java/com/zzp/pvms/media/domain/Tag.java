package com.zzp.pvms.media.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 标签定义
 */
@Data
@TableName("tag")
public class Tag {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String name;

    /** 类型：1-手动，2-AI生成 */
    private Integer tagType;

    /** 维度：人物、地点、食物等 */
    private String category;

    @TableLogic
    private Integer delFlag;
}
