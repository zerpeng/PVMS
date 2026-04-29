package com.zzp.pvms.processor.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_task")
public class SystemTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskType;
    private Integer status;
    private Integer progress;
    private String params;
    private String errorMsg;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
