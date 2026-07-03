package com.cx.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 权限验证结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResult {
    private boolean granted;
    private Map<String, List<String>> dimensionFilters;
    private String permissionCode;
}
