package com.cx.crm.order.config;

import com.cx.crm.order.filter.GatewayTokenInterceptor;
import com.cx.crm.order.interceptor.TenantContextInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private GatewayTokenInterceptor gatewayTokenInterceptor;
    @Autowired
    private TenantContextInterceptor tenantContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 💡 极为关键：第一优先执行网关身份与越权盾牌核验
        registry.addInterceptor(gatewayTokenInterceptor).addPathPatterns("/**");
        // 第二优先挂载多租户 ThreadLocal 环境
        registry.addInterceptor(tenantContextInterceptor).addPathPatterns("/**");
    }
}

