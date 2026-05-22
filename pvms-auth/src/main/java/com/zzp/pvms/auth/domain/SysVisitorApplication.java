package com.zzp.pvms.auth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_visitor_application")
public class SysVisitorApplication {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String applicantName;

    private String reason;

    private Long targetAlbumId;

    private Integer status;

    private String authCode;

    private LocalDateTime expireTime;

    private String roleKey;

    private String auditUsername;

    private String auditRemark;

    private LocalDateTime createTime;

    private LocalDateTime auditTime;
}
