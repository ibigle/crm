package com.cx.admin.controller;

import com.cx.admin.entity.User;
import com.cx.admin.service.UserLoginApplicationService;
import com.cx.admin.vo.LoginResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserLoginApplicationService loginApplicationService;

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
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Internal Server Error"));
        }
    }

    @GetMapping("/user/list")
    public ResponseEntity<List<User>> findUserList() {
        final List<User> userList = loginApplicationService.findUserList();
        return ResponseEntity.ok(userList);
    }
}

