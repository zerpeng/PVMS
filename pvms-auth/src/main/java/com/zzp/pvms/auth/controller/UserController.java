package com.zzp.pvms.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzp.pvms.auth.domain.dto.UserRequest;
import com.zzp.pvms.auth.domain.vo.UserImportResult;
import com.zzp.pvms.auth.domain.vo.UserVO;
import com.zzp.pvms.auth.service.SysUserService;
import com.zzp.pvms.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth/users")
public class UserController {

    @Autowired
    private SysUserService sysUserService;

    @GetMapping
    public Result<Page<UserVO>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String roleKey,
            @RequestParam(required = false) Integer status) {
        return Result.success(sysUserService.queryPage(current, size, username, nickname, roleKey, status));
    }

    @PostMapping
    public Result<UserVO> create(@RequestBody UserRequest request) {
        return Result.success("用户创建成功", sysUserService.create(request));
    }

    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id, @RequestBody UserRequest request) {
        return Result.success("用户更新成功", sysUserService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Name", required = false) String currentUsername) {
        return Result.success("用户删除成功", sysUserService.remove(id, currentUsername));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String roleKey,
            @RequestParam(required = false) Integer status) {
        byte[] bytes = sysUserService.exportCsv(username, nickname, roleKey, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("users.csv", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserImportResult> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.success("用户导入完成", sysUserService.importCsv(file));
    }

    // Role-based permission checks can be added here after the role/permission module is completed.
}
