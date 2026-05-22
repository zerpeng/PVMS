package com.zzp.pvms.auth.service;

import com.zzp.pvms.auth.domain.SysUser;
import com.zzp.pvms.auth.domain.dto.AuditApplicationRequest;
import com.zzp.pvms.auth.domain.dto.RegisterRequest;
import com.zzp.pvms.auth.domain.vo.VisitorApplicationVO;

import java.util.List;

public interface VisitorApplicationService {
    void createFromRegister(RegisterRequest request, SysUser user);

    List<VisitorApplicationVO> list(Integer status);

    long pendingCount();

    VisitorApplicationVO audit(Long id, AuditApplicationRequest request, String auditor);
}
