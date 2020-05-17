package com.alibaba.rsocket.broker;

import com.alibaba.rsocket.encoding.RSocketEncodingFacade;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
        //checking encoder first
        //noinspection ResultOfMethodCallIgnored
        RSocketEncodingFacade.getInstance();
        SpringApplication.run(AlibabaRSocketBrokerServer.class, args);
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "alibaba-rsocket-broker");
    }

    /*@Bean
    public RSocketListenerCustomizer websocketListenerCustomizer() {
        return builder -> {
            builder.listen("ws", 19999);
        };
    }*/
}
