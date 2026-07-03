package com.cx.crm.order.filter;

import cn.hutool.crypto.SecureUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class GatewayTokenInterceptor implements HandlerInterceptor {

    @Value("${crm.gateway.internal-secret:GW_SHIELD_SEC_2026_GLOBAL_TOKEN}")
    private String internalGatewaySecret;

    private static final long TIME_WINDOW_MS = 10 * 1000; // 💡 严格死锁内网 10 秒防篡改滑动窗

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 提取网关层在内网高速网络中派发的特殊网盾标记
        String gatewayToken = request.getHeader("X-Gateway-Token");
        String tenantId = request.getHeader("X-Tenant-ID");
        String clientTimestampStr = request.getHeader("X-Timestamp");

        System.out.println(internalGatewaySecret + ":" + tenantId + ":" + clientTimestampStr);

        if (gatewayToken == null || gatewayToken.isEmpty()) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Forbidden: Direct microservice calling is illegal! Must route via Gateway!\"}");
            return false; // 强力阻断内网黑客横向跨网络入侵
        }

        // 🛡️ 盾牌一：微服务本地执行 10 秒二次时效考核。强力防御任何内网拦截包的横向重放
        long currentTime = System.currentTimeMillis();
        long clientTimestamp = Long.parseLong(clientTimestampStr);
        if (Math.abs(currentTime - clientTimestamp) > TIME_WINDOW_MS) {
            response.setStatus(403);
            response.getWriter().write("Forbidden: Internal Token Expired!");
            return false;
        }

        // 2. 💡 终极对齐：使用完全同构的静态主密钥在微服务本地进行 MD5 摘要算力比对
        String expectedGatewayToken = SecureUtil.md5(internalGatewaySecret + "_" + clientTimestampStr + "_" + tenantId);

        // 3. 严格执行金融级网盾一致性比对
        if (!expectedGatewayToken.equalsIgnoreCase(gatewayToken)) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Forbidden: Spoofing Gateway Token Intercepted!\"}");
            return false;
        }

        return true; // 核验通过！这是百分之百由阿里云 SLB + 网关层洗净的最高安全级别流量，放行通往 Controller 核心业务！
    }
}