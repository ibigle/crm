package com.cx.common.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 授权策略实体（对应表 AUTHORIZATION_POLICY_T）
 */
@Data
public class AuthorizationPolicy {
    private Long policyId;
    private String principalType;      // USER, ROLE, GROUP
    private Long principalId;
    private String permissionCode;
    private Map<String, Object> dimensionFilters;  // JSON 格式
    private String policyEffect;       // ALLOW, DENY
    private String validFrom;
    private String validTo;
    private Long tenantId;
    private String createdBy;
    private LocalDateTime createdDt;
    private String updatedBy;
    private LocalDateTime updatedDt;
}
