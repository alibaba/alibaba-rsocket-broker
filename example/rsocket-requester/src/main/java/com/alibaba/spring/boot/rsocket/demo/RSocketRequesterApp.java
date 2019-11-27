package com.alibaba.spring.boot.rsocket.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RSocket requester app
 *
 * @author leijuan
 */
@SpringBootApplication
public class RSocketRequesterApp {

    public static void main(String[] args) {
        SpringApplication.run(RSocketRequesterApp.class, args);
    }

}
