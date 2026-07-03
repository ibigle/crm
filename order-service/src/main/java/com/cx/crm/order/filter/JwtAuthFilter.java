//package com.cx.orderservice.filter;
//
//import com.cx.orderservice.util.*;
//import org.springframework.cloud.gateway.filter.*;
//import org.springframework.core.*;
//import org.springframework.core.io.buffer.*;
//import org.springframework.http.*;
//import org.springframework.http.server.reactive.*;
//import org.springframework.stereotype.*;
//import org.springframework.web.server.*;
//import reactor.core.publisher.*;
//
//import java.nio.charset.*;
//
//@Component
//public class JwtAuthFilter implements GlobalFilter, Ordered {
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        String path = exchange.getRequest().getURI().getPath();
//
//        // 跳过登录等公开接口
//        if (path.contains("/auth/login")) {
//            return chain.filter(exchange);
//        }
//
//        // 验证 Token
//        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
//        if (token == null || !token.startsWith("Bearer ")) {
//            return unauthorized(exchange, "未提供认证令牌");
//        }
//
//        try {
//            // 解析 JWT 并传递用户信息到下游服务
//            String userId = JwtUtil.parseToken(token.substring(7));
//            ServerHttpRequest request = exchange.getRequest().mutate()
//                    .header("X-User-Id", userId)
//                    .build();
//            return chain.filter(exchange.mutate().request(request).build());
//        } catch (Exception e) {
//            return unauthorized(exchange, "令牌无效或已过期");
//        }
//    }
//
//    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
//        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
//        String body = "{\"code\":401,\"message\":\"" + message + "\"}";
//        DataBuffer buffer = exchange.getResponse().bufferFactory()
//                .wrap(body.getBytes(StandardCharsets.UTF_8));
//        return exchange.getResponse().writeWith(Mono.just(buffer));
//    }
//
//    @Override
//    public int getOrder() {
//        return 0;
//    }
//}
//
