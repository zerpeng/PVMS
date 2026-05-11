package com.zzp.pvms.media.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 监控目录配置
 */
@Data
@TableName("monitor_directory")
public class MonitorDirectory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String dirPath;

    private Integer isRecursive;

    /**
     * 扫描状态：0-待机，1-扫描中
     */
    private Integer scanStatus;

    private LocalDateTime lastScanTime;

    @TableLogic
    private Integer delFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
