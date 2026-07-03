package com.cx.crm.gateway.balancer;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MetricsCollector {

    // 无锁队列，主线程写入性能高
    private final ConcurrentLinkedQueue<MetricEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean flushSignal = new AtomicBoolean(false);

    public void record(String host, int port, long responseTime, boolean success) {
        MetricEvent event = new MetricEvent(host, port, System.currentTimeMillis(),
                responseTime, success);
        eventQueue.offer(event);

        // 通知后台线程处理
        flushSignal.set(true);
    }

    public MetricEvent poll() {
        return eventQueue.poll();
    }

    public boolean shouldFlush() {
        return flushSignal.getAndSet(false);
    }

    @Data
    @AllArgsConstructor
    public static class MetricEvent {
        private String host;
        private int port;
        private long timestamp;
        private long responseTime;
        private boolean success;
    }
}
