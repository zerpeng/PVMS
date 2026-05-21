package com.zzp.pvms.auth.controller;

import com.zzp.pvms.auth.domain.dto.RoleRequest;
import com.zzp.pvms.auth.domain.vo.RoleVO;
import com.zzp.pvms.auth.service.SysRoleService;
import com.zzp.pvms.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/roles")
public class RoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @GetMapping
    public Result<List<RoleVO>> list() {
        return Result.success(sysRoleService.listRoles());
    }

    @PostMapping
    public Result<RoleVO> create(@RequestBody RoleRequest request) {
        return Result.success("角色创建成功", sysRoleService.create(request));
    }

    @PutMapping("/{id}")
    public Result<RoleVO> update(@PathVariable Long id, @RequestBody RoleRequest request) {
        return Result.success("角色更新成功", sysRoleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(sysRoleService.remove(id));
    }
}
