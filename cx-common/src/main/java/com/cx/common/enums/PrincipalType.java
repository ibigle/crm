package com.cx.common.enums;

/**
 * 授权主体类型
 */
public enum PrincipalType {
    USER("用户"),
    ROLE("角色"),
    GROUP("用户组");

    private final String description;

    PrincipalType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
