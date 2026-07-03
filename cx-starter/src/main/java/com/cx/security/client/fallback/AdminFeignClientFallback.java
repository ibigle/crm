package com.cx.security.client.fallback;

import com.cx.common.entity.AuthorizationPolicy;
import com.cx.common.entity.PermissionDefinition;
import com.cx.security.client.AdminFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class AdminFeignClientFallback implements FallbackFactory<AdminFeignClient> {

    @Override
    public AdminFeignClient create(Throwable cause) {
        return new AdminFeignClient() {
            @Override
            public List<AuthorizationPolicy> fetchPolicies(String userId, String permissionCode) {
                log.warn("fetchPolicies fallback, userId={}, permCode={}", userId, permissionCode, cause);
                return Collections.emptyList();
            }

            @Override
            public void sendPermissionDefinitions(List<PermissionDefinition> definitions) {
                log.warn("sendPermissionDefinitions fallback", cause);
            }
        };
    }
}
