package com.zzp.pvms.media.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzp.pvms.common.dto.MediaProcessMsg;
import com.zzp.pvms.media.domain.MediaFile;

import java.util.List;
import java.util.Map;

public interface MediaFileService extends IService<MediaFile> {
    /**
     * 处理元数据提取任务
     * @param msg 消息
     */
    void processMetadata(MediaProcessMsg msg);

    /**
     * 分页查询媒体文件
     */
    Page<MediaFile> queryPage(int current, int size, Integer mediaType);

    /**
     * 获取时间线聚合数据 (按日期分组计数)
     */
    List<Map<String, Object>> getTimeLine();
}
