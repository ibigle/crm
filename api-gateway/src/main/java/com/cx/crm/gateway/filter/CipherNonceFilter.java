package com.cx.crm.gateway.filter;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SecureUtil;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import com.alicp.jetcache.template.QuickConfig;
import com.cx.crm.gateway.util.ExchangeUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 第 3 关【防重放】
 * 安全规范：Order 锁死在 -900
 */
@Component
public class CipherNonceFilter implements GlobalFilter, Ordered {
    @CreateCache(name = "gateway:ddos:", cacheType = CacheType.BOTH, expire = 5)
    private Cache<String, Boolean> nonceCache;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.contains("/crypto/") || path.contains("/public/") || path.contains("/auth/login")) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String sessionId = request.getHeaders().getFirst("X-Session-ID");

        Flux<DataBuffer> cachedBodyFlux = request.getBody().map(dataBuffer -> {
            byte[] rawChunk = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(rawChunk);
            DataBufferUtils.release(dataBuffer);

            String chunkNonce = SecureUtil.md5(HexUtil.encodeHexStr(rawChunk));
            exchange.getAttributes().put("LAZY_CHUNK_NONCE", chunkNonce);

            return new DefaultDataBufferFactory().wrap(rawChunk);
        });

        return Mono.delay(Duration.ofNanos(1)).flatMap(tick -> {
            String nonce = (String) exchange.getAttributes().get("LAZY_CHUNK_NONCE");
            if (nonce == null) {
                return chain.filter(exchange);
            }

            String redisDdosKey = "nonce:" + sessionId + ":" + nonce;
            // JetCache 无锁抢占：putIfAbsent 成功说明该 Nonce 之前未被消费，若不成功说明是闪电重放洗劫！
            boolean success = nonceCache.putIfAbsent(redisDdosKey, Boolean.TRUE);
            if (!success) {
                return ExchangeUtils.setExchangeErrorResponse(exchange, "401: Malicious replay attack detected! Nonce re-consumed!", HttpStatus.UNAUTHORIZED);
            }

            ServerHttpRequest decorator = new ServerHttpRequestDecorator(request) {
                @Override
                public Flux<DataBuffer> getBody() {
                    return cachedBodyFlux; // 来自外部定义的 Flux<DataBuffer>
                }
            };
            return chain.filter(exchange.mutate().request(decorator).build());
        });
    }

    @Override
    public int getOrder() {
        return -900;
    }
}

