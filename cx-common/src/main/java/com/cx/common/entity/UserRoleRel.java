package com.cx.common.entity;

import lombok.Data;

/**
 * 用户-角色关系（对应表 USER_ROLE_REL_T）
 */
@Data
public class UserRoleRel {
    private Long userId;
    private Long roleId;
    private Long tenantId;
}