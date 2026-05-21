package com.zzp.pvms.media.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 相册定义
 */
@Data
@TableName("album")
public class Album {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    /** 类型：1-普通相册，2-智能相册 */
    private Integer albumType;

    /** 智能相册规则(JSON格式) */
    private String queryRule;

    private Long coverMediaId;

    private Long createUserId;

    private String createUsername;

    private LocalDateTime createTime;

    @TableLogic
    private Integer delFlag;
}
