package com.cx.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private Long tenantId;
}
