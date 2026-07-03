package com.cx.crm.gateway.balancer;

import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SlidingWindowStats {

    private static final int BUFFER_SIZE = 1024;
    private final RequestRecord[] ringBuffer = new RequestRecord[BUFFER_SIZE];
    private int writeIndex = 0;
    private int count = 0;

    private final LongAdder totalRtSum = new LongAdder();
    private final LongAdder successCount = new LongAdder();
    private final LongAdder totalCount = new LongAdder();
    private final LongAdder activeRequests = new LongAdder();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final long windowSizeMs;

    public SlidingWindowStats(long windowSizeMs) {
        this.windowSizeMs = windowSizeMs;
    }

    public void record(long responseTime, boolean success) {
        long now = System.currentTimeMillis();

        totalRtSum.add(responseTime);
        totalCount.increment();
        if (success) {
            successCount.increment();
        }

        lock.writeLock().lock();
        try {
            ringBuffer[writeIndex] = new RequestRecord(now, responseTime, success);
            writeIndex = (writeIndex + 1) % BUFFER_SIZE;
            if (count < BUFFER_SIZE) {
                count++;
            }
            if (totalCount.sum() % 100 == 0) {
                cleanupExpired(now);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void incrementActiveRequests() {
        activeRequests.increment();
    }

    public void decrementActiveRequests() {
        activeRequests.decrement();
    }

    private void cleanupExpired(long now) {
        long cutoff = now - windowSizeMs;
        int cleanupCount = Math.min(count, BUFFER_SIZE);

        for (int i = 0; i < cleanupCount; i++) {
            int index = (writeIndex - count + i + BUFFER_SIZE) % BUFFER_SIZE;
            RequestRecord record = ringBuffer[index];

            if (record != null && record.timestamp < cutoff) {
                totalRtSum.add(-record.responseTime);
                totalCount.decrement();
                if (record.success) {
                    successCount.decrement();
                }
                ringBuffer[index] = null;
            }
        }
    }

    public StatsSnapshot getSnapshot() {
        long total = totalCount.sum();
        if (total == 0) {
            return new StatsSnapshot(1.0, 0, 0, activeRequests.sum());
        }

        return new StatsSnapshot(
                (double) successCount.sum() / total,
                totalRtSum.sum() / total,
                total,
                activeRequests.sum()
        );
    }

    private static class RequestRecord {
        final long timestamp;
        final long responseTime;
        final boolean success;

        RequestRecord(long timestamp, long responseTime, boolean success) {
            this.timestamp = timestamp;
            this.responseTime = responseTime;
            this.success = success;
        }
    }
}
