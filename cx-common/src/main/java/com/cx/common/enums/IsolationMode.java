package com.cx.common.enums;

/**
 * 多租户隔离模式
 */
public enum IsolationMode {
    COLUMN("字段隔离"),
    SCHEMA("模式隔离");

    private final String description;

    IsolationMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
