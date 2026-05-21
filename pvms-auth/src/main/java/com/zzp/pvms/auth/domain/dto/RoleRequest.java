package com.zzp.pvms.auth.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleRequest {
    private String roleKey;
    private String roleName;
    private Integer status;
    private Boolean superAdmin;
    private Boolean visitor;
    private List<String> menuKeys;
    private List<Long> directoryIds;
    private List<Long> albumIds;
    private List<Long> tagIds;
    private String remark;
}
