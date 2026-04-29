package com.zzp.pvms.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.media.domain.MonitorDirectory;
import com.zzp.pvms.media.mapper.MonitorDirectoryMapper;
import com.zzp.pvms.media.service.MonitorDirectoryService;
import com.zzp.pvms.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Slf4j
@Service
public class MonitorDirectoryServiceImpl extends ServiceImpl<MonitorDirectoryMapper, MonitorDirectory> implements MonitorDirectoryService {

    @Override
    public boolean addDirectory(MonitorDirectory directory) {
        String path = directory.getDirPath();
        File file = new File(path);
        
        // 1. 校验路径是否存在且是目录
        if (!file.exists() || !file.isDirectory()) {
            throw new ApiException("路径不存在或不是一个有效的目录: " + path);
        }

        // 2. 检查是否已经存在相同路径
        long count = count(new LambdaQueryWrapper<MonitorDirectory>()
                .eq(MonitorDirectory::getDirPath, path));
        if (count > 0) {
            throw new ApiException("该目录已在监控列表中: " + path);
        }

        log.info("添加监控目录: {}", path);
        return save(directory);
    }

    @Override
    public List<MonitorDirectory> listActiveDirectories() {
        return list(new LambdaQueryWrapper<MonitorDirectory>()
                .orderByDesc(MonitorDirectory::getCreateTime));
    }
}
