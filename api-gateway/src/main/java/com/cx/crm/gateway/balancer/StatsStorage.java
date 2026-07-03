package com.cx.crm.gateway.balancer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class StatsStorage {

    private final RedisTemplate<String, String> redisTemplate;
    private final ConcurrentHashMap<String, SlidingWindowStats> localCache = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    // Redis Key前缀
    private static final String SUCCESS_COUNT_KEY = "lb:stats:success:";
    private static final String TOTAL_COUNT_KEY = "lb:stats:total:";
    private static final String ACTIVE_REQUESTS_KEY = "lb:stats:active:";

    public StatsStorage(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateStats(MetricsCollector.MetricEvent event) {
        String key = event.getHost() + ":" + event.getPort();

        if (redisAvailable) {
            try {
                updateRedisStats(key, event);
            } catch (Exception e) {
                log.warn("Redis unavailable, switching to local cache", e);
                redisAvailable = false;
                updateLocalStats(key, event);
            }
        } else {
            updateLocalStats(key, event);
            // 定期尝试恢复Redis连接
            tryRecoverRedis();
        }
    }

    private void updateRedisStats(String key, MetricsCollector.MetricEvent event) {
        String successKey = SUCCESS_COUNT_KEY + key;
        String totalKey = TOTAL_COUNT_KEY + key;
        String activeKey = ACTIVE_REQUESTS_KEY + key;

        // 使用Redis的INCR原子操作
        redisTemplate.opsForValue().increment(totalKey);
        if (event.isSuccess()) {
            redisTemplate.opsForValue().increment(successKey);
        }

        // 设置过期时间（2小时窗口）
        redisTemplate.expire(successKey, 2, TimeUnit.HOURS);
        redisTemplate.expire(totalKey, 2, TimeUnit.HOURS);
    }

    private void updateLocalStats(String key, MetricsCollector.MetricEvent event) {
        SlidingWindowStats stats = localCache.computeIfAbsent(key,
                k -> new SlidingWindowStats(7200000)); // 2小时窗口
        stats.record(event.getResponseTime(), event.isSuccess());
    }

    public StatsSnapshot getStats(String host, int port) {
        String key = host + ":" + port;

        if (redisAvailable) {
            try {
                return getRedisStats(key);
            } catch (Exception e) {
                redisAvailable = false;
                return getLocalStats(key);
            }
        }

        return getLocalStats(key);
    }

    private StatsSnapshot getRedisStats(String key) {
        String successKey = SUCCESS_COUNT_KEY + key;
        String totalKey = TOTAL_COUNT_KEY + key;
        String activeKey = ACTIVE_REQUESTS_KEY + key;

        Long successCount = Long.parseLong(redisTemplate.opsForValue().get(successKey));
        Long totalCount = Long.parseLong(redisTemplate.opsForValue().get(totalKey));
        Long activeRequests = Long.parseLong(redisTemplate.opsForValue().get(activeKey));

        if (totalCount == null || totalCount == 0) {
            return new StatsSnapshot(1.0, 0, 0, 0);
        }

        return new StatsSnapshot(
                (double) (successCount != null ? successCount : 0) / totalCount,
                0, // Redis中不存储RT
                totalCount,
                activeRequests != null ? activeRequests : 0
        );
    }

    private StatsSnapshot getLocalStats(String key) {
        SlidingWindowStats stats = localCache.get(key);
        if (stats == null) {
            return new StatsSnapshot(1.0, 0, 0, 0);
        }
        return stats.getSnapshot();
    }

    private void tryRecoverRedis() {
        try {
            redisTemplate.opsForValue().get("health:check");
            redisAvailable = true;
            log.info("Redis connection recovered");
        } catch (Exception ignored) {
        }
    }
}

