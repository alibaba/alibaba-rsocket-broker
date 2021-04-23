package com.alibaba.rsocket.upstream;

import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.client.SimpleRSocketRequesterSupport;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.discovery.DiscoveryService;
import com.alibaba.rsocket.discovery.RSocketServiceInstance;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCallerImpl;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.extra.processor.TopicProcessor;

import java.util.*;

/**
 * Upstream Manager mock for unit test
 *
 * @author leijuan
 */
public class UpstreamManagerMock implements UpstreamManager {
    private final Map<String, UpstreamCluster> clusters = new HashMap<>();
    private UpstreamCluster brokerCluster;
    private RSocketRequesterSupport rsocketRequesterSupport;
    private RSocket mockRSocket;

    public UpstreamManagerMock() {
        this.rsocketRequesterSupport = new SimpleRSocketRequesterSupport("MockApp", "".toCharArray(),
                Collections.EMPTY_LIST,
                new LocalReactiveServiceCallerImpl(),
                TopicProcessor.<CloudEventImpl>builder().name("cloud-events-processor").build());
        mockRSocket = new RSocket() {
            @Override
            @NotNull
            public Mono<Void> fireAndForget(@NotNull Payload payload) {
                return Mono.empty();
            }

            @Override
            @NotNull
            public Mono<Payload> requestResponse(@NotNull Payload payload) {
                return Mono.empty();
            }

            @Override
            @NotNull
            public Flux<Payload> requestStream(@NotNull Payload payload) {
                return Flux.empty();
            }

            @Override
            @NotNull
            public Flux<Payload> requestChannel(@NotNull Publisher<Payload> payloads) {
                return Flux.empty();
            }

            @Override
            @NotNull
            public Mono<Void> metadataPush(@NotNull Payload payload) {
                return Mono.empty();
            }
        };
        this.add(new UpstreamCluster("", "*", ""));
        // Execute init method
        try {
            init();
        } catch (Exception ignore) {

        }
    }

    @Override
    public void add(UpstreamCluster cluster) {
        clusters.put(cluster.getServiceId(), cluster);
        if (cluster.isBroker()) {
            this.brokerCluster = cluster;
        }
    }

    @Override
    public Collection<UpstreamCluster> findAllClusters() {
        return clusters.values();
    }

    @Override
    public UpstreamCluster findClusterByServiceId(String serviceId) {
        return this.brokerCluster;
    }

    @Override
    public UpstreamCluster findBroker() {
        return this.brokerCluster;
    }

    @Override
    public DiscoveryService findBrokerDiscoveryService() {
        return new DiscoveryService() {
            @Override
            public Flux<RSocketServiceInstance> getInstances(String serviceId) {
                return Flux.empty();
            }

            @Override
            public Flux<String> getAllServices() {
                return Flux.empty();
            }
        };
    }

    @Override
    public RSocket getRSocket(String serviceId) {
        return this.mockRSocket;
    }

    @Override
    public RSocketRequesterSupport requesterSupport() {
        return this.rsocketRequesterSupport;
    }

    @Override
    public void refresh(String serviceId, List<String> uris) {

    }

    @Override
    public void init() throws Exception {
        for (UpstreamCluster cluster : clusters.values()) {
            cluster.setRsocketAware(rsocketRequesterSupport);
            cluster.init();
        }
    }

    @Override
    public void close() {

    }
}
