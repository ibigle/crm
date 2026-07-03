package com.cx.crm.order.interceptor;

import com.cx.crm.order.config.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader("X-Tenant-ID");
        // 强行清洗，并注入到线程池安全的 TransmittableThreadLocal 中
        TenantContext.setTenantId(tenantId != null ? tenantId : "PUBLIC");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear(); // 💡 每次请求完毕在线程还回线程池前原地清洗焚毁，斩断串账流毒
    }
}


