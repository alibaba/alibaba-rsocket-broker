package com.alibaba.spring.boot.rsocket.broker.smi.impl;

import com.alibaba.spring.boot.rsocket.broker.smi.TrafficAccessControl;
import com.alibaba.spring.boot.rsocket.broker.smi.TrafficSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Traffic Access Control implementation
 *
 * @author leijuan
 */
public class TrafficAccessControlImpl implements TrafficAccessControl {
    @Override
    public Mono<Void> grantService(String appName, String... service) {
        return null;
    }

    @Override
    public Mono<Void> grantServiceAccount(String appName, String... serviceAccounts) {
        return null;
    }

    @Override
    public Flux<String> findGrantedServices(String appName) {
        return null;
    }

    @Override
    public Mono<Boolean> checkTraffic(TrafficSpec trafficSpec) {
        return Mono.just(true);
    }
}
