package com.cx.crm.gateway;

import com.cx.crm.gateway.balancer.ConfigurableSmartLoadBalancer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;

/**
 * # 强力指定 Netty 内存分配器采用高性能池化模式
 * -Dio.netty.allocator.type=pooled
 * # 强制缩短 Netty 线程局部块的缓存清理窗口（高并发大文件必备，加速DirectMemory回收）
 * -Dio.netty.allocator.maxOrder=9
 * # 允许 JVM 使用的堆外最大直接内存大小限制（按需调整）
 * -XX:MaxDirectMemorySize=4g
 */
@EnableDiscoveryClient
@SpringBootApplication
@LoadBalancerClients(defaultConfiguration = {ConfigurableSmartLoadBalancer.class})
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
