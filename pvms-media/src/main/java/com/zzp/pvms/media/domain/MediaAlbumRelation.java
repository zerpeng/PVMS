package com.zzp.pvms.media.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 媒体与相册关联关系
 */
@Data
@TableName("media_album_relation")
public class MediaAlbumRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long mediaId;
    private Long albumId;
}
