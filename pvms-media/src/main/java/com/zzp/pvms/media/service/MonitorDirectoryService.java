package com.zzp.pvms.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzp.pvms.media.domain.AuthContext;
import com.zzp.pvms.media.domain.MonitorDirectory;

import java.util.List;

public interface MonitorDirectoryService extends IService<MonitorDirectory> {
    MonitorDirectory addDirectory(MonitorDirectory directory, AuthContext authContext);

    List<MonitorDirectory> listActiveDirectories(AuthContext authContext);

    List<MonitorDirectory> listByIdsForScan(List<Long> ids, AuthContext authContext);
}
