package com.alibaba.spring.boot.rsocket.broker.impl;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.discovery.DiscoveryService;
import com.alibaba.rsocket.discovery.RSocketServiceInstance;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBroker;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
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
    @Autowired
    private RSocketBrokerManager rsocketBrokerManager;

    @Override
    public Flux<RSocketServiceInstance> getInstances(String serviceId) {
        if (serviceId.equals("*")) {
            return Flux.fromIterable(rsocketBrokerManager.currentBrokers())
                    .filter(RSocketBroker::isActive)
                    .map(broker -> {
                        RSocketServiceInstance instance = new RSocketServiceInstance();
                        instance.setInstanceId(broker.getId());
                        instance.setHost(broker.getIp());
                        instance.setServiceId("*");
                        instance.setPort(broker.getPort());
                        instance.setSchema(broker.getSchema());
                        instance.setUri(broker.getUrl());
                        instance.setSecure(broker.isActive());
                        return instance;
                    });
        }
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
        if (appMetadata.getWebPort() > 0) {
            serviceInstance.setPort(appMetadata.getWebPort());
            String schema = "http";
            serviceInstance.setSecure(appMetadata.isSecure());
            if (appMetadata.isSecure()) {
                schema = "https";
            }
            serviceInstance.setSchema(schema);
            serviceInstance.setUri(schema + "://" + appMetadata.getIp() + ":" + appMetadata.getWebPort());
        }
        serviceInstance.setMetadata(appMetadata.getMetadata());
        return serviceInstance;
    }

}
