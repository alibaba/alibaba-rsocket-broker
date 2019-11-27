package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamManager;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * rsocket endpoint for spring boot actuator
 *
 * @author leijuan
 */
@Endpoint(id = "rsocket")
public class RSocketEndpoint {
    private RSocketProperties properties;
    private LocalReactiveServiceCaller localReactiveServiceCaller;
    private UpstreamManager upstreamManager;

    public RSocketEndpoint(RSocketProperties properties, UpstreamManager upstreamManager, LocalReactiveServiceCaller localReactiveServiceCaller) {
        this.properties = properties;
        this.upstreamManager = upstreamManager;
    }

    @ReadOperation
    public Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("id", RSocketAppContext.ID);
        if (!localReactiveServiceCaller.findAllServices().isEmpty()) {
            info.put("published", localReactiveServiceCaller.findAllServices());
        }
        Collection<UpstreamCluster> upstreamClusters = upstreamManager.findAllClusters();
        if (!upstreamClusters.isEmpty()) {
            info.put("subscribed", upstreamClusters.stream().map(upstreamCluster -> {
                Map<String, Object> temp = new HashMap<>();
                temp.put("service", upstreamCluster.getServiceId());
                temp.put("uris", upstreamCluster.getUris());
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
            return Mono.just("Succeed to online!");
        } else if ("offline".equalsIgnoreCase(action)) {
            return Mono.just("Succeed to offline!");
        } else if ("shutdown".equalsIgnoreCase(action)) {
            return Mono.just("Succeed to shutdown!")
                    .delayElement(Duration.ofSeconds(15))
                    .doOnNext(s -> {
                        //todo shutdown logic
                    });
        } else {
            return Mono.just("Unknown action, please use online, offline and shutdown");
        }
    }


}
