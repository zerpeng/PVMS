package com.zzp.pvms.auth.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class AuditApplicationRequest {
    private Boolean approved;
    private String roleName;
    private Boolean superAdmin;
    private Boolean visitor;
    private List<String> menuKeys;
    private List<Long> directoryIds;
    private List<Long> albumIds;
    private List<Long> tagIds;
    private String remark;
}
