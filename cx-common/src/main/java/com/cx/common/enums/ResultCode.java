package com.cx.common.enums;

import lombok.Getter;

@Getter
public enum ResultCode {

    // 通用成功
    SUCCESS(200, "操作成功"),

    // 通用客户端错误 (400-499)
    BAD_REQUEST(400, "请求参数有误"),
    UNAUTHORIZED(401, "未认证，请登录"),
    FORBIDDEN(403, "无权限访问该资源"),
    NOT_FOUND(404, "请求资源不存在"),

    // 通用服务端错误 (500-599)
    INTERNAL_ERROR(500, "系统繁忙，请稍后再试"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 业务自定义错误 (1000+)
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "用户名或密码错误"),
    PERMISSION_DENIED(1003, "您没有该操作的数据权限"),
    DUPLICATE_KEY(1004, "数据重复，请检查");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}