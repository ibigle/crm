package com.cx.crm.gateway.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class IPUtils {

    private static final String IP_UNKNOWN = "unknown";
    private static final String IP_LOCAL = "127.0.0.1";
    private static final int IP_LEN = 15;
    /**
     * 获取客户端真实IP
     */
    public static String getClientIp(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String ip = headers.getFirst("x-forwarded-for");

        if (ip == null || ip.length() == 0 || IP_UNKNOWN.equalsIgnoreCase(ip)) {
            ip = headers.getFirst("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || IP_UNKNOWN.equalsIgnoreCase(ip)) {
            ip = headers.getFirst("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || IP_UNKNOWN.equalsIgnoreCase(ip)) {
            ip = Optional.ofNullable(request.getRemoteAddress())
                    .map(address -> address.getAddress().getHostAddress())
                    .orElse("");
            if (IP_LOCAL.equals(ip)) {
                try {
                    InetAddress inet = InetAddress.getLocalHost();
                    ip = inet.getHostAddress();
                } catch (UnknownHostException e) {
                    // ignore
                }
            }
        }

        // 多个代理时，第一个IP为客户端真实IP
        if (ip != null && ip.length() > IP_LEN) {
            int index = ip.indexOf(",");
            if (index > 0) {
                ip = ip.substring(0, index);
            }
        }

        return ip;
    }
}
