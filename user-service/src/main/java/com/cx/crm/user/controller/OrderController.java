package com.cx.crm.user.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/order")
public class OrderController {

    @RequestMapping("/getOrderNo")
    public String getOrderNo(){
        return "user-service" + UUID.randomUUID().toString();
    }
}
