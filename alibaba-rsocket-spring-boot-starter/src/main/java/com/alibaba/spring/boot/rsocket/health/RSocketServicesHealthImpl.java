package com.alibaba.spring.boot.rsocket.health;

import com.alibaba.rsocket.health.RSocketServiceHealth;
import reactor.core.publisher.Mono;

/**
 * rsocket services health implementation
 *
 * @author leijuan
 */
public class RSocketServicesHealthImpl implements RSocketServiceHealth {
    @Override
    public Mono<Integer> check(String serviceName) {
        //todo implement health logic
        // 如果服务名为空或者empty，则调用应用的health
        return Mono.just(1);
    }
}
