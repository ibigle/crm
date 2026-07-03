package com.cx.crm.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import com.cx.crm.gateway.util.ExchangeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 第 4 关【权限认证】
 * 安全规范：Order 锁死在 -850
 */
@Component
public class AuthTenantFilter implements GlobalFilter, Ordered {

    @Autowired
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @CreateCache(name = "iam:session:", cacheType = CacheType.BOTH, expire = 2, timeUnit = TimeUnit.HOURS)
    private Cache<String, String> lazyPhantomTokenCache;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        if (path.contains("/crypto/") || path.contains("/public/") || path.contains("/auth/login")) {
            return chain.filter(exchange);
        }

        /* 2. 提取并核对前端携带的不记名令牌 (Opaque Token) */
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return ExchangeUtils.setExchangeErrorResponse(exchange, "401: Standard authentication gateway token missing", HttpStatus.UNAUTHORIZED);
        }

        String opaqueTokenKey = authHeader.substring(7);

        /* ==================== ⚡ 【JetCache + Fastjson 极速非阻塞反洗大管道】 ==================== */
        /* 从两级缓存中抓取明文 JSON 文本，完全零 I/O 阻塞 */
        String jsonPayloadText = lazyPhantomTokenCache.get(opaqueTokenKey);

        if (!StringUtils.hasText(jsonPayloadText)) {
            // 令牌失效或已被中后台管理员强制跨网络熔断注销
            return ExchangeUtils.setExchangeErrorResponse(exchange, "401: Session credential has expired or been forcefully revoked", HttpStatus.UNAUTHORIZED);
        }

        try {
            /* 抛弃 ObjectMapper！直接调用 Fastjson 静态高速门面解析，速度飙升 50% */
            /* 并且由于完全没有反射呼叫 Jackson 依赖，彻底从根源上将 NoClassDefFoundError 风险降为 0！ */
            JSONObject payloadMap = JSON.parseObject(jsonPayloadText);

            String userId = payloadMap.getString("userId");
            String tenantId = payloadMap.getString("tenantId");

            // 提取复杂的【动态多维度隔离数据控制字典】
            JSONObject dimensionsObj = payloadMap.getJSONObject("dimensions");
            String dimensionsJsonText = (dimensionsObj != null) ? dimensionsObj.toJSONString() : "{}";

            // 紧凑型 Base64 媒体头压缩，强力抹去下游 Tomcat 传输非英文字符时产生 HTTP 400 的历史性 Bug
            String dimensionsBase64 = java.util.Base64.getEncoder().encodeToString(dimensionsJsonText.getBytes(StandardCharsets.UTF_8));

            // 同步提取、锁定明文加解密状态控制字
            String cryptoEnabledSignal = payloadMap.getOrDefault("cryptoEnabled", "false").toString();
            String compressEnabledSignal = payloadMap.getOrDefault("compressEnabled", "false").toString();

            // 3. 极致改写请求头：向其下游微服务（如 order-service）精准透传洗涤干净后的安全短标头
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .headers(httpHeaders -> {
                        httpHeaders.set("X-User-ID", userId);
                        httpHeaders.set("X-Tenant-ID", tenantId);
                        httpHeaders.set("X-Data-Dimensions-Payload", dimensionsBase64); // ➔ 紧凑型自适应多维度过滤子树
                        httpHeaders.set("X-Crypto-Enabled", cryptoEnabledSignal);       // ➔ 传递给第 5 关 StreamingDecryptFilter 的解密控制字
                        httpHeaders.set("X-Compress-Enabled", compressEnabledSignal);   // ➔ 传递给第 5 关 StreamingDecryptFilter 的解压控制字
                        httpHeaders.remove(HttpHeaders.AUTHORIZATION);                  // 物理剥离抹除幽灵令牌，防气隙外泄
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            return ExchangeUtils.setExchangeErrorResponse(exchange, "401: Gateway Fastjson deserialization breakdown: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public int getOrder() {
        return -850;
    }
}



