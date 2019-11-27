package com.alibaba.spring.boot.rsocket.broker.impl;

import com.alibaba.rsocket.discovery.DiscoveryService;
import com.alibaba.rsocket.discovery.RSocketServiceInstance;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.alibaba.spring.boot.rsocket.broker.supporting.RSocketLocalService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import java.util.Collection;

/**
 * discovery service implementation
 *
 * @author leijuan
 */
@RSocketLocalService(serviceInterface = DiscoveryService.class)
public class DiscoveryServiceImpl implements DiscoveryService {
    @Autowired
    private RSocketBrokerHandlerRegistry handlerRegistry;
    @Autowired
    private ServiceRoutingSelector routingSelector;

    @Override
    public Flux<RSocketServiceInstance> getInstances(String serviceId) {
        return findServiceInstances(serviceId);
    }

    @Override
    public Flux<String> getAllServices() {
        return Flux.fromIterable(routingSelector.findAllServices());
    }

    @NotNull
    private Flux<RSocketServiceInstance> findServiceInstances(String serviceId) {
        return Flux.fromIterable(handlerRegistry.findAll())
                .filter(handler -> handler.getAppMetadata().getName().equalsIgnoreCase(serviceId))
                .filter(handler -> handler.getAppStatus().equals(AppStatusEvent.STATUS_SERVING))
                .map(handler -> {
                    AppMetadata appMetadata = handler.getAppMetadata();
                    RSocketServiceInstance serviceInstance = new RSocketServiceInstance();
                    serviceInstance.setInstanceId(appMetadata.getUuid());
                    serviceInstance.setServiceId(appMetadata.getName());
                    serviceInstance.setHost(appMetadata.getIp());
                    serviceInstance.setPort(appMetadata.getPort());
                    serviceInstance.setSchema(appMetadata.getSchema());
                    serviceInstance.setSecure(appMetadata.isSecure());
                    serviceInstance.setUri(appMetadata.getUri());
                    serviceInstance.setMetadata(appMetadata.getMetadata());
                    return serviceInstance;
                });
    }
}
