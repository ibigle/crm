package com.cx.crm.order.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/order")
public class OrderController {

    @RequestMapping("/getOrderNo")
    @ResponseBody
    public String getOrderNo() {
        return UUID.randomUUID().toString();
    }

    @PostMapping("/v2/getOrderNo")
    @ResponseBody
    public String getOrderNo2(@RequestBody Map<String, String> orderNO) {
        System.out.println("前端传的参数" + orderNO);

        try {
            /* 🌟 强弹性防空对齐：前端传的是 orderNO，后端 params.get 必须区分大小写 */
            String orderNo = orderNO.get("orderNO");
            if (orderNo == null) {
                orderNo = orderNO.get("orderNo"); /* 容错自愈 */
            }

            if (orderNo == null) {
                orderNo = "UNKNOWN_ORDER_NO_ERR"; /* 兜底 */
            }

            System.out.println("核心业务参数 orderNo: " + orderNo);

            /* 🌟 锁死返回格式：必须包装为标准的明文 JSON 字符串写回，决不允许返回原始 null */
            return "{\"success\":true,\"orderNo\":\"" + orderNo + "\"}";

        } catch (
                Exception e) {
            return "{\"success\":false,\"message\":\"Order-Service Parser Error\"}";
        }
    }
}
