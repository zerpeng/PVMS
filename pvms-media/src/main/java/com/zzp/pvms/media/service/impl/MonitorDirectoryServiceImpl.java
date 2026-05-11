package com.zzp.pvms.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.common.exception.ApiException;
import com.zzp.pvms.media.domain.MonitorDirectory;
import com.zzp.pvms.media.mapper.MonitorDirectoryMapper;
import com.zzp.pvms.media.service.MonitorDirectoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class MonitorDirectoryServiceImpl extends ServiceImpl<MonitorDirectoryMapper, MonitorDirectory> implements MonitorDirectoryService {

    @Override
    public MonitorDirectory addDirectory(MonitorDirectory directory) {
        String path = directory.getDirPath();
        File file = new File(path);

        if (!file.exists() || !file.isDirectory()) {
            throw new ApiException("Path does not exist or is not a valid directory: " + path);
        }

        if (directory.getScanStatus() == null) {
            directory.setScanStatus(0);
        }
        if (directory.getIsRecursive() == null) {
            directory.setIsRecursive(1);
        }
        if (directory.getCreateTime() == null) {
            directory.setCreateTime(LocalDateTime.now());
        }

        MonitorDirectory existingDirectory = baseMapper.selectAnyByPath(path);
        if (existingDirectory != null) {
            if (Integer.valueOf(0).equals(existingDirectory.getDelFlag())) {
                throw new ApiException("该目录已存在，不能再次添加");
            }
            return restoreDirectory(existingDirectory, directory);
        }

        log.info("Add monitor directory: {}", path);
        save(directory);
        return directory;
    }

    @Override
    public List<MonitorDirectory> listActiveDirectories() {
        return list(new LambdaQueryWrapper<MonitorDirectory>()
                .orderByDesc(MonitorDirectory::getCreateTime));
    }

    @Override
    public List<MonitorDirectory> listByIdsForScan(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<MonitorDirectory>()
                .in(MonitorDirectory::getId, ids));
    }

    private MonitorDirectory restoreDirectory(MonitorDirectory existingDirectory, MonitorDirectory requestDirectory) {
        existingDirectory.setIsRecursive(requestDirectory.getIsRecursive());
        existingDirectory.setScanStatus(requestDirectory.getScanStatus());
        existingDirectory.setLastScanTime(null);
        existingDirectory.setUpdateTime(LocalDateTime.now());
        existingDirectory.setDelFlag(0);

        log.info("Restore monitor directory: {}", existingDirectory.getDirPath());
        updateById(existingDirectory);
        return getById(existingDirectory.getId());
    }
}
