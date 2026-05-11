package com.zzp.pvms.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzp.pvms.media.domain.UserMediaPreference;
import com.zzp.pvms.media.domain.vo.UserMediaPreferenceVO;

public interface UserMediaPreferenceService extends IService<UserMediaPreference> {
    UserMediaPreferenceVO getPreference(String username);

    UserMediaPreferenceVO updateSyncScan(String username, Boolean syncScanEnabled);
}
