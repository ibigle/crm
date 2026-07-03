package com.cx.security.config;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableMethodCache(basePackages = "com.cx.security")
@EnableCreateCacheAnnotation
public class JetCacheConfig {
}