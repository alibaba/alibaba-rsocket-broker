package com.alibaba.rsocket.broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Alibaba RSocket Broker Server
 *
 * @author leijuan
 */
@SpringBootApplication
public class AlibabaRSocketBrokerServer {
    public static LocalDateTime STARTED_AT = LocalDateTime.now();

    public static void main(String[] args) {
        //BlockHound.install();
        SpringApplication.run(AlibabaRSocketBrokerServer.class, args);
    }

    @Bean
    Flux<Long> fiveSecondsTimer() {
        return Flux.interval(Duration.ofSeconds(3));
    }

}
