package com.cx.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限定义实体（对应表 PERMISSION_DEFINITION_T）
 */
@Data
public class PermissionDefinition {
    private Long permissionId;
    private String permissionCode;
    private String module;
    private String permissionName;
    private String permissionDescription;
    private String serviceName;
    private String className;
    private String methodName;
    private String createdBy;
    private LocalDateTime createdDt;
    private String updatedBy;
    private LocalDateTime updatedDt;
}
