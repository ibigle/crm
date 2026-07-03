package com.cx.security.service;

import com.cx.common.entity.PermissionDefinition;
import com.cx.security.annotation.Permission;
import com.cx.security.client.AdminFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 权限元数据收集器
 * 应用启动时扫描所有带有 @Permission 的 Bean，上报给 permission-admin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionMetaCollector implements ApplicationListener<ContextRefreshedEvent> {

    private final AdminFeignClient adminFeignClient;
    private final Environment environment;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 只处理根上下文，避免重复执行
        if (event.getApplicationContext().getParent() != null) {
            return;
        }

        String serviceName = environment.getProperty("spring.application.name", "unknown");
        Map<String, Object> beans = event.getApplicationContext().getBeansWithAnnotation(Permission.class);

        if (beans.isEmpty()) {
            log.debug("未发现任何标注了 @Permission 的 Bean");
            return;
        }

        List<PermissionDefinition> definitions = new ArrayList<>();

        for (Object bean : beans.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            Permission classPerm = targetClass.getAnnotation(Permission.class);
            String module = classPerm != null ? classPerm.module() : "";

            for (Method method : targetClass.getMethods()) {
                Permission methodPerm = method.getAnnotation(Permission.class);
                if (methodPerm != null) {
                    PermissionDefinition def = new PermissionDefinition();
                    def.setPermissionCode(methodPerm.code());
                    def.setModule(methodPerm.module().isEmpty() ? module : methodPerm.module());
                    def.setPermissionName(methodPerm.name());
                    def.setServiceName(serviceName);
                    def.setClassName(targetClass.getName());
                    def.setMethodName(method.getName());
                    definitions.add(def);
                }
            }
        }

        if (!definitions.isEmpty()) {
            try {
                adminFeignClient.sendPermissionDefinitions(definitions);
                log.info("权限元数据上报成功: 服务名={}, 数量={}", serviceName, definitions.size());
            } catch (Exception e) {
                log.warn("权限元数据上报失败，将在下次应用启动时重试: {}", e.getMessage());
            }
        }
    }
}
