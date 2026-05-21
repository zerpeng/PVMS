package com.zzp.pvms.auth.service;

import com.zzp.pvms.auth.domain.SysRole;
import com.zzp.pvms.auth.domain.dto.RoleRequest;
import com.zzp.pvms.auth.domain.vo.RoleVO;

import java.util.List;

public interface SysRoleService {
    List<RoleVO> listRoles();

    RoleVO create(RoleRequest request);

    RoleVO update(Long id, RoleRequest request);

    boolean remove(Long id);

    SysRole getActiveRole(String roleKey);
}
