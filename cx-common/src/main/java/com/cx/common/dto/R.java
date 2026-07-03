package com.cx.common.dto;

import com.cx.common.enums.ResultCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)  // 字段为 null 时不序列化（减少带宽）
public class R<T> {
    private final int code;
    private final String msg;
    private final T data;
    private final long timestamp;
    private final String traceId;

    // 私有构造器，强制使用静态工厂方法
    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = Instant.now().toEpochMilli();
        // 从 SLF4J MDC 中获取 TraceId（需在网关或过滤器中注入）
        // this.traceId = MDC.get("traceId"); todo: 需要补充
        this.traceId = "";
    }

    // 成功（带数据）
    public static <T> R<T> success(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    // 成功（无数据，仅提示）
    public static <T> R<T> success() {
        return success(null);
    }

    // 失败（使用枚举）
    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    // 失败（自定义消息）
    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    // 失败（自定义消息，常用）
    public static <T> R<T> fail(String msg) {
        return fail(ResultCode.INTERNAL_ERROR.getCode(), msg);
    }
}