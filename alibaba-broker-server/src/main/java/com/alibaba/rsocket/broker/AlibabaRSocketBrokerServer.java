package com.alibaba.rsocket.broker;

import com.alibaba.rsocket.RSocketExchange;
import com.alibaba.rsocket.route.RSocketFilter;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Alibaba RSocket Broker Server
 *
 * @author leijuan
 */
@SpringBootApplication
public class AlibabaRSocketBrokerServer implements DisposableBean {
    public static final LocalDateTime STARTED_AT = LocalDateTime.now();

    @Autowired
    private RSocketBrokerManager brokerManager;

    public static void main(String[] args) {
        //BlockHound.install();
        SpringApplication.run(AlibabaRSocketBrokerServer.class, args);
    }

    @Override
    public void destroy() throws Exception {
        brokerManager.stopLocalBroker();
    }

    @Bean
    @ConditionalOnExpression(value = "false")
    public RSocketFilter demoRSocketFilter() {
        return new RSocketFilter() {
            @Override
            public Mono<Boolean> shouldFilter(RSocketExchange exchange) {
                return Mono.just(true);
            }

            @Override
            public Mono<Void> run(RSocketExchange exchange) {
                System.out.println("call: " + exchange.getFrameType().name());
                return Mono.empty();
            }
        };
    }
}
