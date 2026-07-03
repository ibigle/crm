package com.cx.security.config;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import com.cx.security.aspect.PermissionAspect;
import com.cx.security.client.AdminFeignClient;
import com.cx.security.filter.TenantFilter;
import com.cx.security.interceptor.DimensionInterceptor;
import com.cx.security.service.AuthorizationService;
import com.cx.security.service.PermissionMetaCollector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.sql.DataSource;

@Configuration
@EnableAspectJAutoProxy
@EnableMethodCache(basePackages = "com.permission.starter")
@EnableCreateCacheAnnotation
@Import({JetCacheConfig.class, FeignHttp2Config.class})
@ConditionalOnProperty(prefix = "permission.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PermissionAutoConfiguration {

    @Bean
    public AuthorizationService authorizationService(AdminFeignClient adminFeignClient) {
        return new AuthorizationService(adminFeignClient);
    }

    @Bean
    public PermissionAspect permissionAspect(AuthorizationService authorizationService) {
        return new PermissionAspect(authorizationService);
    }

    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnClass(OncePerRequestFilter.class)
    public TenantFilter tenantFilter() {
        return new TenantFilter();
    }

    @Bean
    @ConditionalOnClass(DataSource.class)
    public DimensionInterceptor dimensionInterceptor() {
        return new DimensionInterceptor();
    }

//    @Bean
//    public PermissionMetaCollector permissionMetaCollector(AdminFeignClient adminFeignClient) {
//        return new PermissionMetaCollector(adminFeignClient);
//    }
}