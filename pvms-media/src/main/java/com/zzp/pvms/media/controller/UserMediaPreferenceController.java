package com.zzp.pvms.media.controller;

import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.domain.dto.SyncScanPreferenceRequest;
import com.zzp.pvms.media.domain.vo.UserMediaPreferenceVO;
import com.zzp.pvms.media.service.UserMediaPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/preferences")
public class UserMediaPreferenceController {

    @Autowired
    private UserMediaPreferenceService userMediaPreferenceService;

    @GetMapping
    public Result<UserMediaPreferenceVO> getPreference(@RequestHeader(value = "X-User-Name", required = false) String username) {
        return Result.success(userMediaPreferenceService.getPreference(username));
    }

    @PutMapping("/sync-scan")
    public Result<UserMediaPreferenceVO> updateSyncScan(
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestBody SyncScanPreferenceRequest request) {
        boolean enabled = request != null && Boolean.TRUE.equals(request.getSyncScanEnabled());
        return Result.success(userMediaPreferenceService.updateSyncScan(username, enabled));
    }
}
