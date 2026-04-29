package com.zzp.pvms.processor.service;

import com.zzp.pvms.common.dto.MediaProcessMsg;

public interface ThumbnailService {
    /**
     * 生成并保存缩略图
     * @param msg 消息内容
     */
    void createThumbnail(MediaProcessMsg msg);
}
