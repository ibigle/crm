package com.cx.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户实体（对应表 TENANT_T）
 */
@Data
public class Tenant {
    private Long tenantId;
    private String tenantCode;
    private String tenantName;
    private String tenantEnName;
    private String isolationMode;      // COLUMN 或 SCHEMA
    private Integer dbType;            // 1-MySQL, 2-PostgreSQL, 3-TiDB
    private String dbHost;
    private String dbPort;
    private String dbSchema;
    private String dbUserName;
    private String dbUserPassword;
    private Integer tenantStatus;      // 1-有效, 0-无效
    private String validFrom;          // YYYYMMDD
    private String validTo;
    private String createdBy;
    private LocalDateTime createdDt;
    private String updatedBy;
    private LocalDateTime updatedDt;
}
