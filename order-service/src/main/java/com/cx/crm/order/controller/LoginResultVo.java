package com.cx.crm.order.controller;

import java.io.Serializable;

/**
 * 💡 统一向前端输出的金融级清洁度登录响应体（VO）
 * ⚖️ 安全规范：100% 拒绝外泄任何 userId、tenantId 或数据维度。对外只暴露 32 字节引用 Token。
 */
public class LoginResultVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String accessToken; // 🌟 寄生、存放不记名幽灵令牌 (Opaque Token / 引用 Token)
    private String tokenType;   // 固定为 "Bearer"
    private long expiresIn;     // 剩余有效死线秒数 (7200秒)

    public LoginResultVo() {
    }

    public LoginResultVo(String accessToken, long expiresIn) {
        this.success = true;
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
    }

    // 标准 Getters/Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}

