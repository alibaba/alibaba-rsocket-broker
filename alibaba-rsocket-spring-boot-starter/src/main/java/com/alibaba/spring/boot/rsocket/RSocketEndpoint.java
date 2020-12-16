package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.events.ServicesHiddenEvent;
import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import com.alibaba.rsocket.loadbalance.LoadBalancedRSocket;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamManager;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * rsocket endpoint for spring boot actuator
 *
 * @author leijuan
 */
@Endpoint(id = "rsocket")
public class RSocketEndpoint {
    private RSocketProperties properties;
    private RSocketRequesterSupport rsocketRequesterSupport;
    private UpstreamManager upstreamManager;
    private Integer rsocketServiceStatus = AppStatusEvent.STATUS_SERVING;
    private Set<String> offlineServices = new HashSet<>();
    private boolean serviceProvider = false;

    public RSocketEndpoint(RSocketProperties properties, UpstreamManager upstreamManager, RSocketRequesterSupport rsocketRequesterSupport) {
        this.properties = properties;
        this.upstreamManager = upstreamManager;
        this.rsocketRequesterSupport = rsocketRequesterSupport;
        Set<ServiceLocator> exposedServices = rsocketRequesterSupport.exposedServices().get();
        if (!exposedServices.isEmpty()) {
            this.serviceProvider = true;
        }
    }

    @ReadOperation
    public Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("id", RSocketAppContext.ID);
        info.put("serviceStatus", AppStatusEvent.statusText(this.rsocketServiceStatus));
        if (this.serviceProvider) {
            info.put("published", rsocketRequesterSupport.exposedServices().get());
        }
        if (!RSocketRemoteServiceBuilder.CONSUMED_SERVICES.isEmpty()) {
            info.put("subscribed", rsocketRequesterSupport.subscribedServices().get().stream()
                    .filter(serviceLocator -> !RSocketServiceHealth.class.getCanonicalName().equals(serviceLocator.getService()))
                    .collect(Collectors.toList()));
        }
        Collection<UpstreamCluster> upstreamClusters = upstreamManager.findAllClusters();
        if (!upstreamClusters.isEmpty()) {
            info.put("upstreams", upstreamClusters.stream().map(upstreamCluster -> {
                Map<String, Object> temp = new HashMap<>();
                temp.put("service", upstreamCluster.getServiceId());
                temp.put("uris", upstreamCluster.getUris());
                LoadBalancedRSocket loadBalancedRSocket = upstreamCluster.getLoadBalancedRSocket();
                temp.put("activeUris", loadBalancedRSocket.getActiveSockets().keySet());
                if (!loadBalancedRSocket.getUnHealthyUriSet().isEmpty()) {
                    temp.put("unHealthyUris", loadBalancedRSocket.getUnHealthyUriSet());
                }
                temp.put("lastRefreshTimeStamp", new Date(loadBalancedRSocket.getLastRefreshTimeStamp()));
                temp.put("lastHealthCheckTimeStamp", new Date(loadBalancedRSocket.getLastHealthCheckTimeStamp()));
                return temp;
            }).collect(Collectors.toList()));
        }
        UpstreamCluster brokerCluster = upstreamManager.findClusterByServiceId("*");
        if (brokerCluster != null) {
            info.put("brokers", brokerCluster.getUris());
        }
        if (properties.getMetadata() != null && !properties.getMetadata().isEmpty()) {
            info.put("metadata", properties.getMetadata());
        }
        if (!offlineServices.isEmpty()) {
            info.put("offlineServices", offlineServices);
        }
        return info;
    }

    @WriteOperation
    public Mono<String> operate(@Selector String action) {
        if ("online".equalsIgnoreCase(action)) {
            this.rsocketServiceStatus = AppStatusEvent.STATUS_SERVING;
            return sendAppStatus(this.rsocketServiceStatus).thenReturn("Succeed to register RSocket services on brokers!");
        } else if (action.startsWith("online-")) {
            String serviceName = action.substring("online-".length());
            ServiceLocator targetService = findServiceLocator(serviceName);
            if (targetService == null) {
                return Mono.just("Service not found:  " + serviceName);
            } else {
                offlineServices.remove(serviceName);
                return sendRegisterService(targetService).thenReturn("Succeed to register " + serviceName + " on brokers!");
            }
        } else if ("offline".equalsIgnoreCase(action)) {
            this.rsocketServiceStatus = AppStatusEvent.STATUS_OUT_OF_SERVICE;
            return sendAppStatus(this.rsocketServiceStatus).thenReturn("Succeed to unregister RSocket services on brokers!");
        } else if (action.startsWith("offline-")) {
            String serviceName = action.substring("offline-".length());
            ServiceLocator targetService = findServiceLocator(serviceName);
            if (targetService == null) {
                return Mono.just("Service not found:  " + serviceName);
            } else {
                offlineServices.add(serviceName);
                return sendUnRegisterService(targetService).thenReturn("Succeed to unregister " + serviceName + " on brokers!");
            }
        } else if ("shutdown".equalsIgnoreCase(action)) {
            this.rsocketServiceStatus = AppStatusEvent.STATUS_STOPPED;
            return sendAppStatus(this.rsocketServiceStatus)
                    .thenReturn("Succeed to unregister RSocket services on brokers! Please wait almost 60 seconds to shutdown the Spring Boot App!");
        } else if ("refreshUpstreams".equalsIgnoreCase(action)) {
            Collection<UpstreamCluster> allClusters = this.upstreamManager.findAllClusters();
            for (UpstreamCluster upstreamCluster : allClusters) {
                upstreamCluster.getLoadBalancedRSocket().refreshUnHealthyUris();
            }
            return Mono.just("Begin to refresh unHealthy upstream clusters now!");
        } else {
            return Mono.just("Unknown action, please use online, offline and shutdown");
        }
    }

    public Mono<Void> sendAppStatus(Integer status) {
        final CloudEventImpl<AppStatusEvent> appStatusEventCloudEvent = RSocketCloudEventBuilder
                .builder(new AppStatusEvent(RSocketAppContext.ID, status))
                .build();
        return Flux.fromIterable(upstreamManager.findAllClusters()).flatMap(upstreamCluster -> upstreamCluster.getLoadBalancedRSocket().fireCloudEventToUpstreamAll(appStatusEventCloudEvent)).then();
    }

    public Mono<Void> sendRegisterService(ServiceLocator targetService) {
        CloudEventImpl<ServicesExposedEvent> cloudEvent = ServicesExposedEvent.convertServicesToCloudEvent(Collections.singletonList(targetService));
        return Flux.fromIterable(upstreamManager.findAllClusters()).flatMap(upstreamCluster -> upstreamCluster.getLoadBalancedRSocket().fireCloudEventToUpstreamAll(cloudEvent)).then();
    }

    public Mono<Void> sendUnRegisterService(ServiceLocator targetService) {
        CloudEventImpl<ServicesHiddenEvent> cloudEvent = ServicesHiddenEvent.convertServicesToCloudEvent(Collections.singletonList(targetService));
        return Flux.fromIterable(upstreamManager.findAllClusters()).flatMap(upstreamCluster -> upstreamCluster.getLoadBalancedRSocket().fireCloudEventToUpstreamAll(cloudEvent)).then();
    }

    /**
     * find service locator
     *
     * @param serviceName service name
     * @return service name
     */
    @Nullable
    private ServiceLocator findServiceLocator(String serviceName) {
        ServiceLocator targetService = null;
        for (ServiceLocator serviceLocator : rsocketRequesterSupport.exposedServices().get()) {
            if (serviceName.equals(serviceLocator.getService())) {
                targetService = serviceLocator;
                break;
            }
        }
        return targetService;
    }

    public Integer getRsocketServiceStatus() {
        return rsocketServiceStatus;
    }

    public boolean isServiceProvider() {
        return serviceProvider;
    }
}
