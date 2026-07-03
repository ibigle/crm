package com.cx.crm.gateway.util;

import com.alibaba.fastjson.JSON;
import com.cx.common.dto.R;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public class ExchangeUtils {
    public static Mono<Void> setExchangeErrorResponse(ServerWebExchange exchange, String msg, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(
                JSON.toJSONString(R.fail(msg)).getBytes(StandardCharsets.UTF_8))));
    }
}
