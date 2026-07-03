package com.cx.crm.gateway;

import lombok.Data;

import java.io.Serializable;

@Data
public class GatewayGlobalPacket implements Serializable {
    private static final long serialVersionUID = 1L;
    // 鉴权令牌 (包含租户信息与权限标识)
    private String token;

    // 签名串 (最内层密文计算的 SM3-HMAC 摘要)
    private String sign;

    // 一次性消费即焚安全放行令牌
    private String oneTimeToken;

    // 明文业务报文的真实媒体类型
    private String contentType;

    // 目标透传转发微服务名
    private String targetService;

    // 真正的给下游微服务自行解密的内层密文
    private String encryptedPayload;
}


