package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import com.alibaba.rsocket.loadbalance.LoadBalancedRSocket;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamManager;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.ZonedDateTime;
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

    public RSocketEndpoint(RSocketProperties properties, UpstreamManager upstreamManager, RSocketRequesterSupport rsocketRequesterSupport) {
        this.properties = properties;
        this.upstreamManager = upstreamManager;
        this.rsocketRequesterSupport = rsocketRequesterSupport;
    }

    @ReadOperation
    public Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("id", RSocketAppContext.ID);
        Set<ServiceLocator> exposedServices = rsocketRequesterSupport.exposedServices().get();
        if (!exposedServices.isEmpty()) {
            info.put("published", exposedServices);
        }
        if (!RSocketRemoteServiceBuilder.CONSUMED_SERVICES.isEmpty()) {
            info.put("subscribed", RSocketRemoteServiceBuilder.CONSUMED_SERVICES);
        }
        Collection<UpstreamCluster> upstreamClusters = upstreamManager.findAllClusters();
        if (!upstreamClusters.isEmpty()) {
            info.put("upstreams", upstreamClusters.stream().map(upstreamCluster -> {
                Map<String, Object> temp = new HashMap<>();
                temp.put("service", upstreamCluster.getServiceId());
                temp.put("uris", upstreamCluster.getUris());
                LoadBalancedRSocket loadBalancedRSocket = upstreamCluster.getLoadBalancedRSocket();
                temp.put("activeUris", loadBalancedRSocket.getActiveSockets().keySet());
                if (!loadBalancedRSocket.getUnHealthUriSet().isEmpty()) {
                    temp.put("unHealthUris", loadBalancedRSocket.getUnHealthUriSet());
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
        return info;
    }

    @WriteOperation
    public Mono<String> operate(@Selector String action) {
        if ("online".equalsIgnoreCase(action)) {
            return sendAppStatus(AppStatusEvent.STATUS_SERVING).thenReturn("Succeed to online!");
        } else if ("offline".equalsIgnoreCase(action)) {
            return sendAppStatus(AppStatusEvent.STATUS_OUT_OF_SERVICE).thenReturn("Succeed to offline!");
        } else if ("shutdown".equalsIgnoreCase(action)) {
            return sendAppStatus(AppStatusEvent.STATUS_STOPPED)
                    .delayElement(Duration.ofSeconds(15))
                    .thenReturn("Succeed to shutdown!");
        } else {
            return Mono.just("Unknown action, please use online, offline and shutdown");
        }
    }


    public Mono<Void> sendAppStatus(Integer status) {
        final CloudEventImpl<AppStatusEvent> appStatusEventCloudEvent = CloudEventBuilder.<AppStatusEvent>builder()
                .withId(UUID.randomUUID().toString())
                .withTime(ZonedDateTime.now())
                .withSource(URI.create("app://" + RSocketAppContext.ID))
                .withType(AppStatusEvent.class.getCanonicalName())
                .withDataContentType("application/json")
                .withData(new AppStatusEvent(RSocketAppContext.ID, status))
                .build();
        return Flux.fromIterable(upstreamManager.findAllClusters()).flatMap(upstreamCluster -> upstreamCluster.fireCloudEventToUpstreamAll(appStatusEventCloudEvent)).then();
    }


}
