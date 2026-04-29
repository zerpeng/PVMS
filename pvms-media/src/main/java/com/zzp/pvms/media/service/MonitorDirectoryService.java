package com.zzp.pvms.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzp.pvms.media.domain.MonitorDirectory;
import java.util.List;

public interface MonitorDirectoryService extends IService<MonitorDirectory> {
    /**
     * 添加监控目录
     * @param directory 目录信息
     * @return 是否成功
     */
    boolean addDirectory(MonitorDirectory directory);

    /**
     * 获取所有有效的监控目录
     * @return 目录列表
     */
    List<MonitorDirectory> listActiveDirectories();
}
