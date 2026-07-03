package com.cx.crm.gateway.filter;


import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import com.cx.crm.gateway.config.GatewayIpResolverConfig;
import com.cx.crm.gateway.util.ExchangeUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 第 2 关【IP黑名单过滤】
 * 安全规范：Order 锁死在 -950
 */
@Component
public class BlacklistFilter implements GlobalFilter, Ordered {
    @CreateCache(name = "gateway:blacklist:", cacheType = CacheType.BOTH, expire = 600)
    private Cache<String, Integer> blacklistCache;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = GatewayIpResolverConfig.getRealClientIp(exchange);
        String blacklistKey = "ip:" + clientIp;

        Integer blockSignal = blacklistCache.get(blacklistKey);
        if (blockSignal != null && blockSignal == 1) {
            return ExchangeUtils.setExchangeErrorResponse(exchange, "Access Denied: IP Banned By DDoS Shield", HttpStatus.FORBIDDEN);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -950;
    }
}

