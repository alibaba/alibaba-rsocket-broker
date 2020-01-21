package com.alibaba.rsocket.health.impl;

import com.alibaba.rsocket.health.RSocketServiceHealth;
import reactor.core.publisher.Mono;

/**
 * RSocket service health default implementation
 *
 * @author leijuan
 */
public class RSocketServiceHealthImpl implements RSocketServiceHealth {
    @Override
    public Mono<Integer> check(String serviceName) {
        return Mono.just(1);
    }
}
