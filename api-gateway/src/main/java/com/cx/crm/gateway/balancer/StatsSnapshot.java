package com.cx.crm.gateway.balancer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatsSnapshot {
    private double successRate;      // 成功率
    private double avgRt;            // 平均响应时间
    private long totalRequests;      // 总请求数
    private long activeRequests;     // 活跃请求数
}

