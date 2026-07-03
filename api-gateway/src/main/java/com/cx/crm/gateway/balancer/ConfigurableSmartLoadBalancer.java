package com.cx.crm.gateway.balancer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.List;

public class ConfigurableSmartLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final ObjectProvider<ServiceInstanceListSupplier> supplierProvider;
    private final StatsStorage statsStorage;
    private final int minSampleSize;

    public ConfigurableSmartLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
            StatsStorage statsStorage,
            @Value("${loadbalancer.min-sample-size:10}")int minSampleSize) {
        this.supplierProvider = supplierProvider;
        this.statsStorage = statsStorage;
        this.minSampleSize = minSampleSize;
    }

    private ServiceInstance selectBestInstance(List<ServiceInstance> instances) {
        ServiceInstance bestInstance = null;
        double highestScore = -1;
        boolean hasValidStats = false;

        for (ServiceInstance instance : instances) {
            StatsSnapshot snapshot = statsStorage.getStats(
                    instance.getHost(), instance.getPort());

            // 检查是否有有效统计数据
            if (snapshot.getTotalRequests() >= minSampleSize) {
                hasValidStats = true;
                double score = calculateScore(snapshot);
                if (score > highestScore) {
                    highestScore = score;
                    bestInstance = instance;
                }
            }
        }

        // 降级策略：拿不到有效统计数据时，走最少请求
        if (!hasValidStats || bestInstance == null) {
            return selectLeastActiveInstance(instances);
        }

        return bestInstance;
    }

    private ServiceInstance selectLeastActiveInstance(List<ServiceInstance> instances) {
        ServiceInstance leastActive = instances.get(0);
        long minActive = Long.MAX_VALUE;

        for (ServiceInstance instance : instances) {
            StatsSnapshot snapshot = statsStorage.getStats(
                    instance.getHost(), instance.getPort());
            long activeRequests = snapshot.getActiveRequests();

            if (activeRequests < minActive) {
                minActive = activeRequests;
                leastActive = instance;
            }
        }

        return leastActive;
    }

    private double calculateScore(StatsSnapshot snapshot) {
        double successScore = snapshot.getSuccessRate() * 100;
        double loadScore = snapshot.getActiveRequests() == 0 ? 100 :
                100.0 / (snapshot.getActiveRequests() + 1);
        return successScore * 0.6 + loadScore * 0.4;
    }

    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = supplierProvider.getIfAvailable();
        if (supplier == null) {
            return Mono.just(new EmptyResponse());
        }

        return supplier.get().next()
                .map(instances -> {
                    if (instances.isEmpty()) {
                        return new EmptyResponse();
                    }
                    ServiceInstance bestInstance = selectBestInstance(instances);
                    return new DefaultResponse(bestInstance);
                });
    }
}
