package com.cx.crm.gateway.controller;

import com.cx.crm.gateway.service.CryptoSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crypto")
public class CryptoController {

    @Autowired
    private CryptoSessionService sessionService;

    @GetMapping("/public-key")
    public String getPublicKey() {
        return sessionService.getServerPublicKeyHex();
    }

    @PostMapping("/negotiate")
    public String negotiate(@RequestParam String sessionId, @RequestParam String clientEncapsulatedKeyHex) {
        sessionService.negotiateSessionKey(sessionId, clientEncapsulatedKeyHex);
        return "SUCCESS";
    }
}
