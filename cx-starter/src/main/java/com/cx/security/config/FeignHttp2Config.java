package com.cx.security.config;

import feign.Client;
//import feign.netty.NettyClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//@ConditionalOnClass(NettyClient.class)
public class FeignHttp2Config {
//    @Bean
//    public Client feignNettyClient() {
//        return new NettyClient();
//    }
}
