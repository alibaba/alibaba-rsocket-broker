package com.alibaba.spring.boot.rsocket.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RSocket Responder Server
 *
 * @author leijuan
 */
@SpringBootApplication
public class RSocketResponderServer {
    public static void main(String[] args) {
        SpringApplication.run(RSocketResponderServer.class, args);
    }
}
