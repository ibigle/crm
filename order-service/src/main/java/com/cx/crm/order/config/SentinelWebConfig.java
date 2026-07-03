//package com.cx.orderservice.config;
//
//import com.alibaba.csp.sentinel.adapter.web.servlet.jakarta.SentinelWebInterceptor;
//import com.alibaba.csp.sentinel.adapter.web.servlet.jakarta.config.SentinelWebMvcConfig;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class SentinelWebConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        // 1. 创建 Sentinel 针对 Jakarta Servlet 的配置
//        SentinelWebMvcConfig config = new SentinelWebMvcConfig();
//        // 开启 URL 资源统计
//        config.setWebContextUnify(true);
//        // 自动将 HTTP 请求方法（GET/POST）作为资源前缀（可选）
//        config.setBlockPage(null);
//
//        // 2. 将 Sentinel 拦截器注册进 Spring Boot 的 MVC 管道中
//        registry.addInterceptor(new SentinelWebInterceptor(config))
//                .addPathPatterns("/**"); // 拦截所有请求
//    }
//}
