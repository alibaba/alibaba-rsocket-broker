package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import com.alibaba.rsocket.upstream.UpstreamManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import reactor.core.publisher.Mono;

/**
 * RSocket Broker health indicator
 *
 * @author leijuan
 */
public class RSocketBrokerHealthIndicator implements ReactiveHealthIndicator {
    private RSocketServiceHealth rsocketServiceHealth;
    private String brokers;


    public RSocketBrokerHealthIndicator(UpstreamManager upstreamManager, String brokers) {
        this.rsocketServiceHealth = RSocketRemoteServiceBuilder
                .client(RSocketServiceHealth.class)
                .upstreamManager(upstreamManager)
                .build();
        this.brokers = brokers;
    }


    @Override
    public Mono<Health> health() {
        return rsocketServiceHealth.check("com.alibaba.rsocket.health.Health")
                .map(result -> result != null && result == 1 ?
                        Health.up().withDetail("brokers", brokers).build()
                        : Health.outOfService().withDetail("brokers", brokers).build())
                .onErrorReturn(Health.down().withDetail("brokers", brokers).build());
    }

}
