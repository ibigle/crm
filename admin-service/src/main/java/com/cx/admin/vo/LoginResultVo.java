package com.cx.admin.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Accessors(chain = true)
@Data
@NoArgsConstructor
public class LoginResultVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String accessToken; // 存放 32 字节幽灵不记名引用令牌
    private String tokenType;   // 固定为 "Bearer"
    private long expiresIn;     // 7200秒

    public LoginResultVo(String accessToken, long expiresIn) {
        this.success = true;
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
    }
}

