package com.zzp.pvms.media.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_media_preference")
public class UserMediaPreference {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private Integer syncScanEnabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
