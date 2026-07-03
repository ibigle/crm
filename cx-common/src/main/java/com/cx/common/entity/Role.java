package com.cx.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体（对应表 ROLE_T）
 */
@Data
public class Role {
    private Long roleId;
    private String roleName;
    private String roleCode;
    private Long tenantId;
    private String createdBy;
    private LocalDateTime createdDt;
    private String updatedBy;
    private LocalDateTime updatedDt;
}
