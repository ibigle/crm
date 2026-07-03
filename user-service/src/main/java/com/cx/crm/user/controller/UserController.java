package com.cx.crm.user.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@RestController
@RequestMapping("/user")
public class UserController {

    @RequestMapping("/getUserNo")
    public Integer getUserNo() throws NoSuchAlgorithmException {
        return SecureRandom.getInstanceStrong().nextInt();
    }
}
