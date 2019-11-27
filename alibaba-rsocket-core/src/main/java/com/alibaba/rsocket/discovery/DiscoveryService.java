package com.alibaba.rsocket.discovery;

import reactor.core.publisher.Flux;

/**
 * discovery service
 *
 * @author leijuan
 */
public interface DiscoveryService {

    Flux<RSocketServiceInstance> getInstances(String serviceId);

    Flux<String> getAllServices();
}
