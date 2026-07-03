package com.cx.common.entity;

import lombok.Data;

/**
 * 用户组-用户关系（对应表 USER_GROUP_REL_T）
 */
@Data
public class UserGroupRel {
    private Long groupId;
    private Long userId;
    private Long tenantId;
}
