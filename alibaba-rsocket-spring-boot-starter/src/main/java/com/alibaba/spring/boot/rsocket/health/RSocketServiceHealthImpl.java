package com.alibaba.spring.boot.rsocket.health;

import com.alibaba.rsocket.RSocketService;
import com.alibaba.rsocket.health.RSocketServiceHealth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
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
    private ReactiveHealthIndicator healthIndicator;

    @Override
    public Mono<Integer> check(String serviceName) {
        return healthIndicator.health()
                .map(Health::getStatus)
                .map(status -> {
                    if (status == Status.UP) {
                        return SERVING_STATUS;
                    } else if (status == Status.DOWN) {
                        return DOWN_STATUS;
                    } else {
                        return UNKNOWN_STATUS;
                    }
                });

    }
}
