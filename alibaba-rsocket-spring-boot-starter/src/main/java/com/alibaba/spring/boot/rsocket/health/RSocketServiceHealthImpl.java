package com.alibaba.spring.boot.rsocket.health;

import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.spring.boot.rsocket.RSocketService;
import reactor.core.publisher.Mono;

/**
 * RSocket service health default implementation
 *
 * @author leijuan
 */
@RSocketService(serviceInterface = RSocketServiceHealth.class)
public class RSocketServiceHealthImpl implements RSocketServiceHealth {
    @Override
    public Mono<Integer> check(String serviceName) {
        return Mono.just(1);
    }
}
