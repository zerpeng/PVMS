package com.zzp.pvms.auth.controller;

import com.zzp.pvms.auth.domain.dto.AuditApplicationRequest;
import com.zzp.pvms.auth.domain.vo.VisitorApplicationVO;
import com.zzp.pvms.auth.service.VisitorApplicationService;
import com.zzp.pvms.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/applications")
public class ApplicationController {

    @Autowired
    private VisitorApplicationService visitorApplicationService;

    @GetMapping
    public Result<List<VisitorApplicationVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestHeader(value = "X-Super-Admin", required = false) String superAdmin,
            @RequestHeader(value = "X-Menu-Keys", required = false) String menuKeys,
            @RequestHeader(value = "X-User-Role", required = false) String roleKey) {
        if (!canAudit(superAdmin, menuKeys, roleKey)) {
            return Result.failed(403, "无访问审核权限");
        }
        return Result.success(visitorApplicationService.list(status));
    }

    @GetMapping("/pending-count")
    public Result<Map<String, Long>> pendingCount(
            @RequestHeader(value = "X-Super-Admin", required = false) String superAdmin,
            @RequestHeader(value = "X-Menu-Keys", required = false) String menuKeys,
            @RequestHeader(value = "X-User-Role", required = false) String roleKey) {
        long count = canAudit(superAdmin, menuKeys, roleKey) ? visitorApplicationService.pendingCount() : 0L;
        return Result.success(Map.of("count", count));
    }

    @PutMapping("/{id}/audit")
    public Result<VisitorApplicationVO> audit(
            @PathVariable Long id,
            @RequestBody AuditApplicationRequest request,
            @RequestHeader(value = "X-User-Name", required = false) String auditor,
            @RequestHeader(value = "X-Super-Admin", required = false) String superAdmin,
            @RequestHeader(value = "X-Menu-Keys", required = false) String menuKeys,
            @RequestHeader(value = "X-User-Role", required = false) String roleKey) {
        if (!canAudit(superAdmin, menuKeys, roleKey)) {
            return Result.failed(403, "无访问审核权限");
        }
        return Result.success("审核完成", visitorApplicationService.audit(id, request, auditor));
    }

    private boolean canAudit(String superAdmin, String menuKeys, String roleKey) {
        if ("ADMIN".equalsIgnoreCase(roleKey) || Boolean.parseBoolean(superAdmin)) {
            return true;
        }
        if (menuKeys == null) {
            return false;
        }
        return List.of(menuKeys.split(",")).stream()
                .map(String::trim)
                .anyMatch(key -> "applications".equals(key) || "roles".equals(key) || "users".equals(key));
    }
}
