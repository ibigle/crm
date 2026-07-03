package com.cx.crm.gateway.config;

import com.cx.crm.gateway.balancer.ConfigurableSmartLoadBalancer;
import com.cx.crm.gateway.balancer.StatsStorage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class SmartLoadBalancerConfig {

    @Value("${loadbalancer.min-sample-size:10}")
    private int minSampleSize;

    @Bean
    @LoadBalanced
    public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory,
            StatsStorage statsStorage) {

        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);

        ObjectProvider<ServiceInstanceListSupplier> provider =
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class);

        return new ConfigurableSmartLoadBalancer(provider, statsStorage, minSampleSize);
    }
}
