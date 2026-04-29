package com.zzp.pvms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 媒体处理消息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaProcessMsg implements Serializable {
    
    /** 媒体文件ID (如果是初次扫描可能为 null) */
    private Long mediaId;
    
    /** 文件绝对路径 */
    private String filePath;
    
    /** 所属监控目录ID */
    private Long directoryId;
    
    /** 
     * 任务类型: 
     * 1: 提取元数据 (Metadata Extraction)
     * 2: 生成缩略图 (Thumbnail Generation)
     * 3: 全部 (默认)
     */
    private Integer taskType;
}
