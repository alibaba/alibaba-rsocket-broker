package com.alibaba.spring.boot.rsocket.broker.impl;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.discovery.DiscoveryService;
import com.alibaba.rsocket.discovery.RSocketServiceInstance;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.alibaba.spring.boot.rsocket.broker.supporting.RSocketLocalService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        return Flux.fromIterable(routingSelector.findAllServices()).map(ServiceLocator::getGsv);
    }

    @NotNull
    private Flux<RSocketServiceInstance> findServiceInstances(String serviceId) {
        Integer serviceHashCode = ServiceLocator.serviceHashCode(serviceId);
        Collection<Integer> instanceIdList = routingSelector.findHandlers(serviceHashCode);
        if (instanceIdList.isEmpty()) {
            return findServiceInstancesByAppName(serviceId);
        }
        List<RSocketServiceInstance> serviceInstances = new ArrayList<>();
        for (Integer handlerId : instanceIdList) {
            RSocketBrokerResponderHandler handler = handlerRegistry.findById(handlerId);
            if (handler != null) {
                serviceInstances.add(constructServiceInstance(handler));
            }
        }
        return Flux.fromIterable(serviceInstances);
    }

    @NotNull
    private Flux<RSocketServiceInstance> findServiceInstancesByAppName(String appName) {
        return Flux.fromIterable(handlerRegistry.findAll())
                .filter(handler -> handler.getAppMetadata().getName().equalsIgnoreCase(appName))
                .filter(handler -> handler.getAppStatus().equals(AppStatusEvent.STATUS_SERVING))
                .map(this::constructServiceInstance);
    }

    @NotNull
    private RSocketServiceInstance constructServiceInstance(RSocketBrokerResponderHandler handler) {
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
    }

}
