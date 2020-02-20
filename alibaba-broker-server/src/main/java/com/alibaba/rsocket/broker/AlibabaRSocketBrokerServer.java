package com.alibaba.rsocket.broker;

import com.alibaba.rsocket.broker.filters.CanaryFilter;
import com.alibaba.rsocket.route.RSocketFilter;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

/**
 * Alibaba RSocket Broker Server
 *
 * @author leijuan
 */
@SpringBootApplication
public class AlibabaRSocketBrokerServer {
    public static final LocalDateTime STARTED_AT = LocalDateTime.now();
    
    public static void main(String[] args) {
        //BlockHound.install();
        SpringApplication.run(AlibabaRSocketBrokerServer.class, args);
    }

    /**
     * canary RSocket filter
     *
     * @return CanaryFilter
     */
    @Bean
    @ConditionalOnExpression(value = "false")
    public RSocketFilter canaryRSocketFilter() {
        return new CanaryFilter();
    }
}
