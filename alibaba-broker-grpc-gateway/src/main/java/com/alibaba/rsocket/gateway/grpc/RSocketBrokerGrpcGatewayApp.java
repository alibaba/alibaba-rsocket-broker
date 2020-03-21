package com.alibaba.rsocket.gateway.grpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * RSocket Broker gRPC Gateway App
 *
 * @author leijuan
 */
@SpringBootApplication
public class RSocketBrokerGrpcGatewayApp implements WebFluxConfigurer {
    public static void main(String[] args) {
        SpringApplication.run(RSocketBrokerGrpcGatewayApp.class, args);
    }
}
