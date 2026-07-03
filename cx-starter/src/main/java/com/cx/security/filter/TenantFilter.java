package com.cx.security.filter;

import com.cx.security.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户解析过滤器
 * 从请求头 X-Tenant-Id 中提取租户标识，存入 ThreadLocal
 * 优先级最高（Ordered.HIGHEST_PRECEDENCE + 1），确保后续所有组件都能获取到租户信息
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 从请求头获取租户标识
        String tenant = request.getHeader("X-Tenant-Id");
        if (!StringUtils.hasText(tenant)) {
            // 若未传递，使用默认租户（可配置）
            tenant = "default";
            log.debug("未传递租户标识，使用默认租户: {}", tenant);
        }

        TenantContext.setTenantId(tenant);
        try {
            chain.doFilter(request, response);
        } finally {
            // 清理 ThreadLocal，防止内存泄漏
            TenantContext.clear();
        }
    }
}