package com.zzp.pvms.auth.domain.vo;

import com.zzp.pvms.auth.domain.SysVisitorApplication;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitorApplicationVO {
    private Long id;
    private Long userId;
    private String username;
    private String applicantName;
    private String reason;
    private Long targetAlbumId;
    private Integer status;
    private String statusText;
    private String roleKey;
    private String auditUsername;
    private String auditRemark;
    private LocalDateTime createTime;
    private LocalDateTime auditTime;

    public static VisitorApplicationVO from(SysVisitorApplication application) {
        VisitorApplicationVO vo = new VisitorApplicationVO();
        vo.setId(application.getId());
        vo.setUserId(application.getUserId());
        vo.setUsername(application.getUsername());
        vo.setApplicantName(application.getApplicantName());
        vo.setReason(application.getReason());
        vo.setTargetAlbumId(application.getTargetAlbumId());
        vo.setStatus(application.getStatus());
        vo.setStatusText(resolveStatusText(application.getStatus()));
        vo.setRoleKey(application.getRoleKey());
        vo.setAuditUsername(application.getAuditUsername());
        vo.setAuditRemark(application.getAuditRemark());
        vo.setCreateTime(application.getCreateTime());
        vo.setAuditTime(application.getAuditTime());
        return vo;
    }

    private static String resolveStatusText(Integer status) {
        if (Integer.valueOf(1).equals(status)) {
            return "已通过";
        }
        if (Integer.valueOf(2).equals(status)) {
            return "已拒绝";
        }
        return "待审核";
    }
}
