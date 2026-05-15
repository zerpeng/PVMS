package com.zzp.pvms.media.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("media_favorite")
public class MediaFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private Long mediaId;

    private LocalDateTime createTime;
}
