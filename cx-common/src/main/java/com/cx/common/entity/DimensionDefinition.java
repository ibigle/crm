package com.cx.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 维度定义实体（对应表 DIMENSION_DEFINITION_T）
 */
@Data
public class DimensionDefinition {
    private Long dimensionId;
    private String dimensionCode;
    private String dimensionName;
    private String valueType;          // STRING, NUMBER, LIST
    private Integer dimensionStatus;   // 1-有效, 0-无效
    private Long tenantId;
    private String createdBy;
    private LocalDateTime createdDt;
    private String updatedBy;
    private LocalDateTime updatedDt;
}
