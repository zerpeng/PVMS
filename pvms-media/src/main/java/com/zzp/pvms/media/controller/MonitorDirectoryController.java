package com.zzp.pvms.media.controller;

import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.domain.AuthContext;
import com.zzp.pvms.media.domain.MonitorDirectory;
import com.zzp.pvms.media.domain.dto.ScanDirectoriesRequest;
import com.zzp.pvms.media.service.FileScannerService;
import com.zzp.pvms.media.service.MonitorDirectoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/directories")
public class MonitorDirectoryController {

    @Autowired
    private MonitorDirectoryService monitorDirectoryService;

    @Autowired
    private FileScannerService fileScannerService;

    @GetMapping
    public Result<List<MonitorDirectory>> list(
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Role", required = false) String roleKey,
            @RequestHeader(value = "X-Super-Admin", required = false) String superAdmin,
            @RequestHeader(value = "X-Visitor", required = false) String visitor,
            @RequestHeader(value = "X-Directory-Ids", required = false) String directoryIds,
            @RequestHeader(value = "X-Album-Ids", required = false) String albumIds,
            @RequestHeader(value = "X-Tag-Ids", required = false) String tagIds) {
        return Result.success(monitorDirectoryService.listActiveDirectories(auth(username, roleKey, superAdmin, visitor, directoryIds, albumIds, tagIds)));
    }

    @PostMapping("/scan/{id}")
    public Result<String> scan(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Role", required = false) String roleKey,
            @RequestHeader(value = "X-Super-Admin", required = false) String superAdmin,
            @RequestHeader(value = "X-Visitor", required = false) String visitor,
            @RequestHeader(value = "X-Directory-Ids", required = false) String directoryIds,
            @RequestHeader(value = "X-Album-Ids", required = false) String albumIds,
            @RequestHeader(value = "X-Tag-Ids", required = false) String tagIds) {
        AuthContext authContext = auth(username, roleKey, superAdmin, visitor, directoryIds, albumIds, tagIds);
        MonitorDirectory directory = monitorDirectoryService.getById(id);
        if (directory == null || monitorDirectoryService.listByIdsForScan(List.of(id), authContext).isEmpty()) {
            return Result.failed("Directory does not exist");
        }
        fileScannerService.scanDirectory(directory);
        return Result.success("Scan task started");
    }

    @PostMapping("/scan")
    public Result<String> scanBatch(
            @RequestBody ScanDirectoriesRequest request,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Role", required = false) String roleKey,
            @RequestHeader(value = "X-Super-Admin", required = false) String superAdmin,
            @RequestHeader(value = "X-Visitor", required = false) String visitor,
            @RequestHeader(value = "X-Directory-Ids", required = false) String directoryIds,
            @RequestHeader(value = "X-Album-Ids", required = false) String albumIds,
            @RequestHeader(value = "X-Tag-Ids", required = false) String tagIds) {
        if (request == null || request.getDirectoryIds() == null || request.getDirectoryIds().isEmpty()) {
            return Result.failed("No directory selected");
        }
        List<MonitorDirectory> directories = monitorDirectoryService.listByIdsForScan(request.getDirectoryIds(), auth(username, roleKey, superAdmin, visitor, directoryIds, albumIds, tagIds));
        if (directories.isEmpty()) {
            return Result.failed("No directory selected");
        }
        directories.forEach(fileScannerService::scanDirectory);
        return Result.success("Started " + directories.size() + " scan task(s)");
    }

    @PostMapping
    public Result<MonitorDirectory> add(
            @RequestBody MonitorDirectory directory,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Role", required = false) String roleKey,
            @RequestHeader(value = "X-Super-Admin", required = false) String superAdmin,
            @RequestHeader(value = "X-Visitor", required = false) String visitor,
            @RequestHeader(value = "X-Directory-Ids", required = false) String directoryIds,
            @RequestHeader(value = "X-Album-Ids", required = false) String albumIds,
            @RequestHeader(value = "X-Tag-Ids", required = false) String tagIds) {
        return Result.success(monitorDirectoryService.addDirectory(directory, auth(username, roleKey, superAdmin, visitor, directoryIds, albumIds, tagIds)));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Role", required = false) String roleKey,
            @RequestHeader(value = "X-Super-Admin", required = false) String superAdmin,
            @RequestHeader(value = "X-Visitor", required = false) String visitor,
            @RequestHeader(value = "X-Directory-Ids", required = false) String directoryIds,
            @RequestHeader(value = "X-Album-Ids", required = false) String albumIds,
            @RequestHeader(value = "X-Tag-Ids", required = false) String tagIds) {
        AuthContext authContext = auth(username, roleKey, superAdmin, visitor, directoryIds, albumIds, tagIds);
        if (!authContext.canWrite()) {
            return Result.failed("访客无删除目录权限");
        }
        if (!authContext.isSuperAdmin() && monitorDirectoryService.listByIdsForScan(List.of(id), authContext).isEmpty()) {
            return Result.failed("目录不存在或无权访问");
        }
        log.info("Remove monitor directory id: {}", id);
        return Result.success(monitorDirectoryService.removeById(id));
    }

    private AuthContext auth(String username, String roleKey, String superAdmin, String visitor, String directoryIds, String albumIds, String tagIds) {
        return AuthContext.of(username, roleKey, superAdmin, visitor, directoryIds, albumIds, tagIds);
    }
}
