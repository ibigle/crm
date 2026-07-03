package com.cx.crm.gateway.balancer;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class StatsProcessor implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StatsProcessor.class);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private MetricsCollector collector;

    @Autowired
    private StatsStorage statsStorage;

    @Override
    public void run(String... args) {
        // 每100ms批量处理一次统计数据
        scheduler.scheduleAtFixedRate(this::processMetrics, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void processMetrics() {
        if (!collector.shouldFlush()) {
            return;
        }

        try {
            // 批量处理队列中的事件
            MetricsCollector.MetricEvent event;
            while ((event = collector.poll()) != null) {
                statsStorage.updateStats(event);
            }
        } catch (Exception e) {
            log.error("Error processing metrics", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
