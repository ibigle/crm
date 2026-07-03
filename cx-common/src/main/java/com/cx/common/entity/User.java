package com.cx.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（对应表 USER_T）
 */
@Data
public class User {
    private Long userId;
    private Long tenantId;
    private String userName;
    private String userPassword;
    private String realName;
    private String phoneNo;
    private String email;
    private Integer userStatus;        // 1-有效, 0-无效, 2-冻结
    private String validFrom;
    private String validTo;
    private String createdBy;
    private LocalDateTime createdDt;
    private String updatedBy;
    private LocalDateTime updatedDt;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime passwordExpireDt;
}
