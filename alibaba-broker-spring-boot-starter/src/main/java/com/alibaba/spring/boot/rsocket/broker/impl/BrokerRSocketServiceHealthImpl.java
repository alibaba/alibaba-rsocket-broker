package com.alibaba.spring.boot.rsocket.broker.impl;

import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
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
    private LocalReactiveServiceCaller localReactiveServiceCaller;
    private ServiceRoutingSelector routingSelector;

    public BrokerRSocketServiceHealthImpl(LocalReactiveServiceCaller localReactiveServiceCaller,
                                          ServiceRoutingSelector routingSelector) {
        this.localReactiveServiceCaller = localReactiveServiceCaller;
        this.routingSelector = routingSelector;
    }

    @Override
    public Mono<Integer> check(String serviceName) {
        if (localReactiveServiceCaller.contains(serviceName)) {
            return Mono.just(1);
        } else {
            return Flux.fromIterable(routingSelector.findAllServices())
                    .any(serviceId -> serviceId.contains("" + serviceName + ":"))
                    .map(result -> result ? 1 : 2);
        }
    }
}
