//package com.cx.orderservice.interceptor;
//
//import com.cx.orderservice.properties.*;
//import jakarta.servlet.http.*;
//import org.springframework.beans.factory.annotation.*;
//import org.springframework.http.*;
//import org.springframework.stereotype.*;
//import org.springframework.web.servlet.*;
//
//@Component
//public class GatewayOnlyInterceptor implements HandlerInterceptor {
//
//    @Autowired
//    private GatewayForceProperties gatewayForceProperties;
//
//    @Override
//    public boolean preHandle(HttpServletRequest request,
//                             HttpServletResponse response,
//                             Object handler) throws Exception {
//
//        // 开关关闭时，允许所有请求
//        if (!gatewayForceProperties.isEnabled()) {
//            return true;
//        }
//
//        // 开关开启时，检查是否来自网关转发
//        String gatewayForward = request.getHeader("X-Gateway-Forward");
//        if (!"true".equals(gatewayForward)) {
//            response.setStatus(HttpStatus.FORBIDDEN.value());
//            response.setContentType("application/json;charset=UTF-8");
//            response.getWriter().write("{\"code\":403,\"message\":\"请通过API网关访问\"}");
//            return false;
//        }
//
//        return true;
//    }
//}
