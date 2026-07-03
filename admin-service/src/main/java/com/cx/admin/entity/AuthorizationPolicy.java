package com.cx.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Accessors(chain = true)
@Data
@TableName("AUTHORIZATION_POLICY_T")
public class AuthorizationPolicy implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long policyId;
    private String principalType;
    private Long principalId;
    private String permissionCode;
    private String dimensionFilters; // 数据库中为 JSON 字段，此处使用 String 无缝接管反解
    private String policyEffect;
    private Long tenantId;
}

