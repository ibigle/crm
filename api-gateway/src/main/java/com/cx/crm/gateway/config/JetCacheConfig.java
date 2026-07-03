package com.cx.crm.gateway.config;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableMethodCache(basePackages = "com.cx.crm.gateway")
@EnableCreateCacheAnnotation
public class JetCacheConfig {
}
