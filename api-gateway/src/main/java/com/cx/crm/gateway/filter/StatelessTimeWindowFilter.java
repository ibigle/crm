package com.cx.crm.gateway.filter;

import com.cx.crm.gateway.util.ExchangeUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 第 1 关【时间窗强防刷网盾】
 * 安全规范：Order 锁死在 -1000，零 I/O 成本，快速解构第一波 DDoS 垃圾流量。
 */
@Component
public class StatelessTimeWindowFilter implements GlobalFilter, Ordered {

    /* 强时钟反刷允许的服务器最大时差代差：严格锁死 10 秒 */
    private static final long ALLOWED_TIME_WINDOW_MS = 10 * 1000L;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String timestampStr = exchange.getRequest().getHeaders().getFirst("X-Timestamp");
        if (!StringUtils.hasText(timestampStr)) {
            return ExchangeUtils.setExchangeErrorResponse(exchange, "401: Clock validation defect, timestamp index missing", HttpStatus.UNAUTHORIZED);
        }

        try {
            long clientTime = Long.parseLong(timestampStr);
            long serverTime = System.currentTimeMillis();

            /* 纯 CPU 栈内无锁数学绝对值比对 */
            if (Math.abs(serverTime - clientTime) > ALLOWED_TIME_WINDOW_MS) {
                return ExchangeUtils.setExchangeErrorResponse(exchange, "401: Time window expired", HttpStatus.UNAUTHORIZED);
            }
        } catch (NumberFormatException e) {
            return ExchangeUtils.setExchangeErrorResponse(exchange, "401: Malformed protocol timestamp payload", HttpStatus.UNAUTHORIZED);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1000;
    }
}
