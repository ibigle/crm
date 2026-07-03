package com.cx.common.dto;

import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
    private Long tenantId;  // 可选，若租户可从用户名解析则可省略
}
