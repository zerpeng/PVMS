package com.zzp.pvms.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzp.pvms.common.exception.ApiException;
import com.zzp.pvms.media.domain.AuthContext;
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
    public MonitorDirectory addDirectory(MonitorDirectory directory, AuthContext authContext) {
        assertWritable(authContext);
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
        directory.setCreateUsername(authContext.getUsername());

        MonitorDirectory existingDirectory = baseMapper.selectAnyByPath(path);
        if (existingDirectory != null) {
            if (Integer.valueOf(0).equals(existingDirectory.getDelFlag())) {
                throw new ApiException("目录已存在，不能重复添加");
            }
            return restoreDirectory(existingDirectory, directory);
        }

        log.info("Add monitor directory: {}", path);
        save(directory);
        return directory;
    }

    @Override
    public List<MonitorDirectory> listActiveDirectories(AuthContext authContext) {
        LambdaQueryWrapper<MonitorDirectory> wrapper = new LambdaQueryWrapper<MonitorDirectory>()
                .orderByDesc(MonitorDirectory::getCreateTime);
        applyVisibility(wrapper, authContext);
        return list(wrapper);
    }

    @Override
    public List<MonitorDirectory> listByIdsForScan(List<Long> ids, AuthContext authContext) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<MonitorDirectory> wrapper = new LambdaQueryWrapper<MonitorDirectory>()
                .in(MonitorDirectory::getId, ids);
        applyVisibility(wrapper, authContext);
        return list(wrapper);
    }

    private void applyVisibility(LambdaQueryWrapper<MonitorDirectory> wrapper, AuthContext authContext) {
        if (authContext.isSuperAdmin()) {
            return;
        }
        List<Long> permittedIds = authContext.getDirectoryIds();
        if (authContext.isVisitor()) {
            if (permittedIds.isEmpty()) {
                wrapper.eq(MonitorDirectory::getId, -1L);
            } else {
                wrapper.in(MonitorDirectory::getId, permittedIds);
            }
            return;
        }

        wrapper.and(scope -> {
            scope.eq(MonitorDirectory::getCreateUsername, authContext.getUsername());
            if (!permittedIds.isEmpty()) {
                scope.or().in(MonitorDirectory::getId, permittedIds);
            }
        });
    }

    private void assertWritable(AuthContext authContext) {
        if (!authContext.canWrite()) {
            throw new ApiException("访客无新增目录权限");
        }
    }

    private MonitorDirectory restoreDirectory(MonitorDirectory existingDirectory, MonitorDirectory requestDirectory) {
        existingDirectory.setIsRecursive(requestDirectory.getIsRecursive());
        existingDirectory.setScanStatus(requestDirectory.getScanStatus());
        existingDirectory.setCreateUsername(requestDirectory.getCreateUsername());
        existingDirectory.setUpdateTime(LocalDateTime.now());

        log.info("Restore monitor directory: {}", existingDirectory.getDirPath());
        baseMapper.restoreById(existingDirectory);
        return getById(existingDirectory.getId());
    }
}
