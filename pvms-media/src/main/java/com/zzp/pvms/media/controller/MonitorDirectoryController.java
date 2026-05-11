package com.zzp.pvms.media.controller;

import com.zzp.pvms.common.api.Result;
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
    public Result<List<MonitorDirectory>> list() {
        return Result.success(monitorDirectoryService.listActiveDirectories());
    }

    @PostMapping("/scan/{id}")
    public Result<String> scan(@PathVariable Long id) {
        MonitorDirectory directory = monitorDirectoryService.getById(id);
        if (directory == null) {
            return Result.failed("Directory does not exist");
        }
        fileScannerService.scanDirectory(directory);
        return Result.success("Scan task started");
    }

    @PostMapping("/scan")
    public Result<String> scanBatch(@RequestBody ScanDirectoriesRequest request) {
        if (request == null || request.getDirectoryIds() == null || request.getDirectoryIds().isEmpty()) {
            return Result.failed("No directory selected");
        }
        List<MonitorDirectory> directories = monitorDirectoryService.listByIdsForScan(request.getDirectoryIds());
        if (directories.isEmpty()) {
            return Result.failed("No directory selected");
        }
        directories.forEach(fileScannerService::scanDirectory);
        return Result.success("Started " + directories.size() + " scan task(s)");
    }

    @PostMapping
    public Result<MonitorDirectory> add(@RequestBody MonitorDirectory directory) {
        return Result.success(monitorDirectoryService.addDirectory(directory));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        log.info("Remove monitor directory id: {}", id);
        return Result.success(monitorDirectoryService.removeById(id));
    }
}
