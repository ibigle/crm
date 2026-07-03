package com.cx.security.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.cx.common.dto.AuthResult;
import com.cx.common.entity.AuthorizationPolicy;
import com.cx.security.client.AdminFeignClient;
import com.cx.security.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthorizationService {

    private final AdminFeignClient adminFeignClient;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Cached(name = "perm:auth:", expire = 300, cacheType = CacheType.BOTH,
            localLimit = 10000, localExpire = 60)
    public AuthResult checkPermission(String userId, String permissionCode) {
        log.debug("Cache miss for userId={}, permCode={}", userId, permissionCode);

        List<AuthorizationPolicy> policies;
        try {
            policies = adminFeignClient.fetchPolicies(userId, permissionCode);
        } catch (Exception e) {
            log.error("Fetch policies failed, fallback to deny", e);
            return new AuthResult(false, Collections.emptyMap(), permissionCode);
        }

        if (policies == null || policies.isEmpty()) {
            return new AuthResult(false, Collections.emptyMap(), permissionCode);
        }

        // DENY 优先
        for (AuthorizationPolicy p : policies) {
            if ("DENY".equals(p.getPolicyEffect()) && isValid(p)) {
                log.warn("用户 {} 在权限 {} 上存在显式拒绝策略", userId, permissionCode);
                return new AuthResult(false, Collections.emptyMap(), permissionCode);
            }
        }

        // 合并 ALLOW 策略的维度（从 AuthorizationPolicy.dimensionFilters 中提取）
        Map<String, List<String>> merged = new HashMap<>();
        for (AuthorizationPolicy p : policies) {
            if ("ALLOW".equals(p.getPolicyEffect()) && isValid(p)) {
                mergeDimension(merged, p.getDimensionFilters());
            }
        }

        // 自动注入租户
        String tenant = TenantContext.getTenantId();
        if (StringUtils.hasText(tenant)) {
            merged.computeIfAbsent("tenant", k -> new ArrayList<>()).add(tenant);
        }

        return merged.isEmpty()
                ? new AuthResult(false, Collections.emptyMap(), permissionCode)
                : new AuthResult(true, merged, permissionCode);
    }

    private boolean isValid(AuthorizationPolicy p) {
        String from = p.getValidFrom();
        String to = p.getValidTo();
        String today = LocalDateTime.now().format(DATE_FORMATTER);
        if (StringUtils.hasText(from) && today.compareTo(from) < 0) return false;
        if (StringUtils.hasText(to) && today.compareTo(to) > 0) return false;
        return true;
    }

    @SuppressWarnings("unchecked")
    private void mergeDimension(Map<String, List<String>> target, Map<String, Object> source) {
        if (source == null) return;
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof List) {
                target.computeIfAbsent(key, k -> new ArrayList<>()).addAll((List<String>) value);
            } else if (value instanceof String) {
                target.computeIfAbsent(key, k -> new ArrayList<>()).add((String) value);
            } else if (value instanceof Number) {
                target.computeIfAbsent(key, k -> new ArrayList<>()).add(String.valueOf(value));
            }
        }
    }
}