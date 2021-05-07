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
import com.alibaba.spring.boot.rsocket.broker.security.RSocketAppPrincipal;
import com.alibaba.spring.boot.rsocket.broker.supporting.RSocketLocalService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    public Mono<List<RSocketServiceInstance>> getInstances(String serviceId) {
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
                    }).collectList();
        }
        return findServiceInstances(serviceId).collectList();
    }

    @Override
    public Mono<List<String>> findAppInstances(String orgId) {
        return Flux.fromIterable(handlerRegistry.findAll())
                .filter(responderHandler -> {
                    RSocketAppPrincipal principal = responderHandler.getPrincipal();
                    return principal != null && principal.getOrganizations().contains(orgId);
                })
                .map(responderHandler -> {
                    AppMetadata appMetadata = responderHandler.getAppMetadata();
                    return appMetadata.getName() + "," + appMetadata.getIp() + "," + appMetadata.getConnectedAt();
                })
                .collectList();
    }

    @Override
    public Mono<List<String>> getAllServices() {
        return Flux.fromIterable(routingSelector.findAllServices()).map(ServiceLocator::getGsv).collectList();
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

    @Override
    public Mono<RSocketServiceInstance> getInstance(String appId) {
        RSocketBrokerResponderHandler responderHandler = handlerRegistry.findByUUID(appId);
        if (responderHandler != null) {
            return Mono.just(constructServiceInstance(responderHandler));
        }
        return Mono.empty();
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
        Map<Integer, String> rsocketPorts = appMetadata.getRsocketPorts();
        if (rsocketPorts != null && !rsocketPorts.isEmpty()) {
            Map.Entry<Integer, String> entry = rsocketPorts.entrySet().stream().findFirst().get();
            try {
                serviceInstance.setPort(entry.getKey());
                serviceInstance.setSchema(entry.getValue());
                serviceInstance.setUri(entry.getValue() + "://" + appMetadata.getIp() + ":" + entry.getKey());
            } catch (Exception ignore) {

            }
        }
        serviceInstance.setMetadata(appMetadata.getMetadata());
        return serviceInstance;
    }

}
