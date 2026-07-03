package com.cx.common.enums;

/**
 * 用户状态
 */
public enum UserStatus {
    ACTIVE(1, "有效"),
    INACTIVE(0, "无效"),
    FROZEN(2, "冻结");

    private final int code;
    private final String description;

    UserStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
