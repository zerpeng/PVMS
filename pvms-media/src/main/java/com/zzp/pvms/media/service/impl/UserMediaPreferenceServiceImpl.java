package com.zzp.pvms.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.media.domain.UserMediaPreference;
import com.zzp.pvms.media.domain.vo.UserMediaPreferenceVO;
import com.zzp.pvms.media.mapper.UserMediaPreferenceMapper;
import com.zzp.pvms.media.service.UserMediaPreferenceService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserMediaPreferenceServiceImpl extends ServiceImpl<UserMediaPreferenceMapper, UserMediaPreference> implements UserMediaPreferenceService {

    private static final int ENABLED = 1;

    @Override
    public UserMediaPreferenceVO getPreference(String username) {
        UserMediaPreference preference = getByUsername(username);
        return new UserMediaPreferenceVO(preference != null && Integer.valueOf(ENABLED).equals(preference.getSyncScanEnabled()));
    }

    @Override
    public UserMediaPreferenceVO updateSyncScan(String username, Boolean syncScanEnabled) {
        UserMediaPreference preference = getByUsername(username);
        int enabled = Boolean.TRUE.equals(syncScanEnabled) ? 1 : 0;

        if (preference == null) {
            preference = new UserMediaPreference();
            preference.setUsername(resolveUsername(username));
            preference.setSyncScanEnabled(enabled);
            save(preference);
        } else {
            preference.setSyncScanEnabled(enabled);
            updateById(preference);
        }

        return new UserMediaPreferenceVO(enabled == ENABLED);
    }

    private UserMediaPreference getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<UserMediaPreference>()
                .eq(UserMediaPreference::getUsername, resolveUsername(username))
                .last("LIMIT 1"));
    }

    private String resolveUsername(String username) {
        return StringUtils.hasText(username) ? username : "anonymous";
    }
}
