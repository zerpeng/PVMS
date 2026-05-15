package com.zzp.pvms.media.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 媒体文件元数据
 */
@Data
@TableName("media_file")
public class MediaFile {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;
    private String filePath;
    private String fileHash;
    private Long fileSize;
    
    /** 1-图片，2-视频 */
    private Integer mediaType;
    private String fileFormat;

    // EXIF 数据
    private LocalDateTime shotTime;
    private Integer width;
    private Integer height;
    private Integer duration;
    private String cameraMake;
    private String cameraModel;
    private String lensModel;
    private String fNumber;
    private String exposureTime;
    private Integer iso;
    private Integer focalLength;

    // 地理位置
    private BigDecimal gpsLatitude;
    private BigDecimal gpsLongitude;
    private String locationName;

    private String thumbnailPath;
    private Long directoryId;

    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private Boolean favorited;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
