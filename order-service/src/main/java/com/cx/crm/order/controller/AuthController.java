package com.cx.crm.order.controller;

import com.cx.crm.order.UserLoginApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 💡 统一身份中台安全登录握手大龙头控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserLoginApplicationService loginApplicationService;

    /**
     * 🚀 登录发放龙头：接收明文或经过国密洗涤后的登录请求，发放幽灵引用 Token
     */
    @PostMapping("/login")
    public ResponseEntity<?> executeTenantLogin(@RequestBody Map<String, String> loginPayload) {
        String username = loginPayload.get("username");
        String password = loginPayload.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing username or password"));
        }

        try {
            LoginResultVo result = loginApplicationService.authenticateAndIssueReferenceToken(username, password);
            return ResponseEntity.ok(result);
        } catch (SecurityException ex) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Internal Server Error: " + e.getMessage()));
        }
    }
}

