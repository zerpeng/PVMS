package com.zzp.pvms.auth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleKey;

    private String roleName;

    private Integer status;

    private Integer superAdmin;

    private Integer visitor;

    private String menuKeys;

    private String directoryIds;

    private String albumIds;

    private String tagIds;

    private String remark;

    private Integer builtin;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
