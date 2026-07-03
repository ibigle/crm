package com.cx.crm.order.filter;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.cx.crm.order.controller")
public class DownstreamSecurityResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true; // 开启全局切面拦截
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        if (response == null || response.getHeaders() == null) {
            return body;
        }

        // 💡 智能策略自适应分流判定：
        if (selectedContentType.isCompatibleWith(MediaType.APPLICATION_OCTET_STREAM) ||
                selectedContentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA) ||
                selectedContentType.isCompatibleWith(MediaType.IMAGE_PNG) ||
                selectedContentType.isCompatibleWith(MediaType.IMAGE_JPEG)) {

            // 💡 针对二进制大文件流：命令网关执行增量流式加密，但直接关闭高延迟的压缩引擎，确保极致性能
            response.getHeaders().set("X-Response-Crypto", "true");
            response.getHeaders().set("X-Response-Compress", "false");
        } else {
            // 💡 针对常规高频的订单业务 JSON 或纯文本：命令网关双开，兼顾体积和传输安全
            response.getHeaders().set("X-Response-Crypto", "true");
            response.getHeaders().set("X-Response-Compress", "true");
        }

        return body; // 业务数据原样无污染向下流动
    }
}
