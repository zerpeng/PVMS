package com.zzp.pvms.media.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzp.pvms.common.dto.MediaProcessMsg;
import com.zzp.pvms.media.domain.AuthContext;
import com.zzp.pvms.media.domain.MediaFile;

import java.util.List;
import java.util.Map;

public interface MediaFileService extends IService<MediaFile> {
    void processMetadata(MediaProcessMsg msg);

    Page<MediaFile> queryPage(int current, int size, Integer mediaType, Long albumId, Long tagId, Boolean favoriteOnly, AuthContext authContext);

    MediaFile getActiveById(Long id, AuthContext authContext);

    List<Map<String, Object>> getTimeLine(AuthContext authContext);
}
