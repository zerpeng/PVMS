package com.zzp.pvms.media.controller;

import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.domain.MonitorDirectory;
import com.zzp.pvms.media.service.FileScannerService;
import com.zzp.pvms.media.service.MonitorDirectoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
            return Result.failed("目录不存在");
        }
        fileScannerService.scanDirectory(directory);
        return Result.success("扫描任务已启动");
    }

    @PostMapping()
    public Result<Boolean> add(@RequestBody MonitorDirectory directory) {
        return Result.success(monitorDirectoryService.addDirectory(directory));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        log.info("移除监控目录ID: {}", id);
        return Result.success(monitorDirectoryService.removeById(id));
    }
}
