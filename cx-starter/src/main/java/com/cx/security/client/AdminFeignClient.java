package com.cx.security.client;


import com.cx.common.entity.AuthorizationPolicy;
import com.cx.common.entity.PermissionDefinition;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "client-service",
        configuration = com.cx.security.config.FeignHttp2Config.class,
        fallbackFactory = com.cx.security.client.fallback.AdminFeignClientFallback.class
)
public interface AdminFeignClient {

    /**
     * 获取用户的授权策略（功能权限 + 数据维度）
     */
    @GetMapping("/internal/policies")
    List<AuthorizationPolicy> fetchPolicies(
            @RequestParam("userId") String userId,
            @RequestParam("permCode") String permissionCode
    );

    /**
     * 上报权限定义元数据
     */
    @PostMapping("/internal/permissions/batch")
    void sendPermissionDefinitions(@RequestBody List<PermissionDefinition> definitions);
}
