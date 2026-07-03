package com.cx.common.enums;

/**
 * 策略效果
 */
public enum PolicyEffect {
    ALLOW("允许"),
    DENY("拒绝");

    private final String description;

    PolicyEffect(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
