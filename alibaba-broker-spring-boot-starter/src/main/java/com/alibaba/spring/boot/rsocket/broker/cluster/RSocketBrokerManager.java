package com.alibaba.spring.boot.rsocket.broker.cluster;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * RSocket Broker Manager
 *
 * @author leijuan
 */
public interface RSocketBrokerManager {

    Flux<Collection<RSocketBroker>> requestAll();

    RSocketBroker localBroker();

    Collection<RSocketBroker> currentBrokers();

    Mono<RSocketBroker> findByIp(String ip);

    Flux<ServiceLocator> findServices(String ip);

    Boolean isStandAlone();

    String getName();

    void stopLocalBroker();

    Mono<String> broadcast(CloudEventImpl<?> cloudEvent);

    RSocketBroker findConsistentBroker(String clientId);
}
