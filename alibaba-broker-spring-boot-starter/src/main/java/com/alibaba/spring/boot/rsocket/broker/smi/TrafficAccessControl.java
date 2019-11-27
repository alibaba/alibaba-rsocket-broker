package com.alibaba.spring.boot.rsocket.broker.smi;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Traffic Access Control
 *
 * @author leijuan
 */
public interface TrafficAccessControl {

    Mono<Void> grantService(String appName, String... service);

    Mono<Void> grantServiceAccount(String appName, String... serviceAccounts);

    Flux<String> findGrantedServices(String appName);

    Mono<Boolean> checkTraffic(TrafficSpec trafficSpec);
}
