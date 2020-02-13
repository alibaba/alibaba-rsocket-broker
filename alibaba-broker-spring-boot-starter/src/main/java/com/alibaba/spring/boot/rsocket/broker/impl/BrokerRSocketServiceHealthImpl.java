package com.alibaba.spring.boot.rsocket.broker.impl;

import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.alibaba.spring.boot.rsocket.broker.supporting.RSocketLocalService;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * rsocket broker service health implement
 *
 * @author leijuan
 */
@RSocketLocalService(serviceInterface = RSocketServiceHealth.class)
public class BrokerRSocketServiceHealthImpl implements RSocketServiceHealth {
    private ServiceRoutingSelector routingSelector;

    public BrokerRSocketServiceHealthImpl(ServiceRoutingSelector routingSelector) {
        this.routingSelector = routingSelector;
    }

    @Override
    public Mono<Integer> check(@Nullable String serviceName) {
        //health check
        if (serviceName == null || serviceName.isEmpty()) {
            return Mono.just(SERVING_STATUS);
        } else { //remote service check
            return Flux.fromIterable(routingSelector.findAllServices())
                    .any(serviceLocator -> serviceLocator.getService().equals(serviceName))
                    .map(result -> result ? SERVING_STATUS : DOWN_STATUS);
        }
    }
}
