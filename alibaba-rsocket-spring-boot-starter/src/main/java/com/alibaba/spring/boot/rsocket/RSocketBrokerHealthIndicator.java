package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.events.AppStatusEvent;
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
    private RSocketEndpoint rsocketEndpoint;
    private String brokers;


    public RSocketBrokerHealthIndicator(RSocketEndpoint rsocketEndpoint, UpstreamManager upstreamManager, String brokers) {
        this.rsocketEndpoint = rsocketEndpoint;
        this.rsocketServiceHealth = RSocketRemoteServiceBuilder
                .client(RSocketServiceHealth.class)
                .upstreamManager(upstreamManager)
                .build();
        this.brokers = brokers;
    }

    @Override
    public Mono<Health> health() {
        return rsocketServiceHealth.check(null)
                .map(result -> {
                            boolean brokerAlive = result != null && result == 1;
                            boolean localServicesAlive = !rsocketEndpoint.getRsocketServiceStatus().equals(AppStatusEvent.STATUS_STOPPED);
                            Health.Builder builder = brokerAlive && localServicesAlive ? Health.up() : Health.outOfService();
                            builder.withDetail("brokers", brokers);
                            builder.withDetail("localServiceStatus", AppStatusEvent.statusText(rsocketEndpoint.getRsocketServiceStatus()));
                            return builder.build();
                        }
                )
                .onErrorReturn(Health.down().withDetail("brokers", brokers).build());
    }

}
