package com.alibaba.spring.boot.rsocket.health;

import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.spring.boot.rsocket.RSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import reactor.core.publisher.Mono;

/**
 * RSocket service health default implementation
 *
 * @author leijuan
 */
@RSocketService(serviceInterface = RSocketServiceHealth.class)
public class RSocketServiceHealthImpl implements RSocketServiceHealth {
    @Autowired
    private HealthEndpoint healthEndpoint;

    @Override
    public Mono<Integer> check(String serviceName) {
        Status status = healthEndpoint.health().getStatus();
        if (status == Status.UP) {
            return Mono.just(SERVING_STATUS);
        } else if (status == Status.DOWN) {
            return Mono.just(DOWN_STATUS);
        } else {
            return Mono.just(UNKNOWN_STATUS);
        }
    }
}
