package com.cx.crm.order.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "gateway.force")
@Component
@Data
public class GatewayForceProperties {
    /**
     * 是否强制只能通过网关访问后端服务
     * true: 后端服务拒绝直接请求
     * false: 后端服务允许直接请求
     */
    private boolean enabled = true;

    // getter/setter
}

