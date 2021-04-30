package com.alibaba.rsocket.discovery;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * discovery service for registry client
 *
 * @author leijuan
 */
public interface DiscoveryService {

    Mono<List<RSocketServiceInstance>> getInstances(String serviceId);

    Mono<List<String>> findAppInstances(String orgId);

    Mono<List<String>> getAllServices();
}
