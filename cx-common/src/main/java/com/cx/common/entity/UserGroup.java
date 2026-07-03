package com.cx.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户组实体（对应表 USER_GROUP_T）
 */
@Data
public class UserGroup {
    private Long groupId;
    private String groupName;
    private Long tenantId;
    private String createdBy;
    private LocalDateTime createdDt;
    private String updatedBy;
    private LocalDateTime updatedDt;
}
