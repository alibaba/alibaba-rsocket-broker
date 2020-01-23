package com.alibaba.spring.boot.rsocket.broker.impl;

import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.alibaba.spring.boot.rsocket.broker.supporting.RSocketLocalService;
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
    public Mono<Integer> check(String serviceName) {
        //health check
        if (serviceName == null || serviceName.equals("com.alibaba.rsocket.health.Health")) {
            return Mono.just(1);
        } else { //remote service check
            return Flux.fromIterable(routingSelector.findAllServices())
                    .any(serviceId -> serviceId.contains("" + serviceName + ":"))
                    .map(result -> result ? 1 : 0);
        }
    }
}
