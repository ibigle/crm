package com.cx.security.aspect;

import com.cx.common.dto.AuthResult;
import com.cx.common.exception.PermissionDeniedException;
import com.cx.security.annotation.Permission;
import com.cx.security.context.TenantContext;
import com.cx.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 功能权限验证 AOP 切面
 * 拦截所有标注了 @Permission 的方法，执行权限验证并将数据维度注入 ThreadLocal
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final AuthorizationService authorizationService;

    @Around("@annotation(permission)")
    public Object checkPermission(ProceedingJoinPoint pjp, Permission permission) throws Throwable {
        String userId = getCurrentUserId();
        String permCode = permission.code();

        log.debug("权限验证: userId={}, permCode={}, method={}",
                userId, permCode, pjp.getSignature().toShortString());

        // 执行权限验证（包含功能权限 + 数据维度）
        AuthResult result = authorizationService.checkPermission(userId, permCode);
        if (!result.isGranted()) {
            log.warn("权限拒绝: userId={}, permCode={}", userId, permCode);
            throw new PermissionDeniedException("无权限执行: " + permCode);
        }

        // 将数据维度注入 ThreadLocal（供 MyBatis 拦截器使用）
        TenantContext.setDimensionFilters(result.getDimensionFilters());

        try {
            return pjp.proceed();
        } finally {
            // 清理维度，防止线程复用导致的数据串扰
            TenantContext.setDimensionFilters(null);
        }
    }

    /**
     * 获取当前用户ID
     * 优先从 Spring Security 上下文获取，若无则返回系统用户
     */
    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
}