package com.alibaba.rsocket.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RSocket Broker HTTP Gateway App
 *
 * @author leijuan
 */
@SpringBootApplication
public class RSocketBrokerHttpGatewayApp {
    public static void main(String[] args) {
        SpringApplication.run(RSocketBrokerHttpGatewayApp.class, args);
    }
}
