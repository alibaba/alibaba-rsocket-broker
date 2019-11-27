package com.alibaba.rsocket.registry.client;

import com.alibaba.rsocket.discovery.DiscoveryService;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RSocket discovery client
 *
 * @author leijuan
 */
@EnableDiscoveryClient
public class RSocketDiscoveryClient implements ReactiveDiscoveryClient {
    private DiscoveryService discoveryService;

    public RSocketDiscoveryClient(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public String description() {
        return "RSocket Discovery Client";
    }

    @Override
    public Flux<ServiceInstance> getInstances(String serviceId) {
        return discoveryService.getInstances(serviceId).map(rsocketInstance -> new DefaultServiceInstance(rsocketInstance.getInstanceId(), rsocketInstance.getServiceId(), rsocketInstance.getHost(), rsocketInstance.getPort(), rsocketInstance.isSecure(), rsocketInstance.getMetadata()));
    }

    @Override
    public Flux<String> getServices() {
        return discoveryService.getAllServices();
    }
}
