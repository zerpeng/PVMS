package com.zzp.pvms.processor.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("media_file")
public class MediaFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String thumbnailPath;
}
