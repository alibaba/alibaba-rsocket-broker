package com.alibaba.rsocket.upstream;

import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.discovery.DiscoveryService;
import com.alibaba.rsocket.discovery.RSocketServiceInstance;
import com.alibaba.rsocket.invocation.RSocketRequesterRpcProxy;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.rsocket.RSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuples;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * upstream manager implementation
 *
 * @author leijuan
 */
public class UpstreamManagerImpl implements UpstreamManager {
    private static final Logger log = LoggerFactory.getLogger(UpstreamManagerImpl.class);
    private final Map<String, UpstreamCluster> clusters = new HashMap<>();
    private UpstreamCluster brokerCluster;
    private DiscoveryService brokerDiscoveryService;
    private RSocketRequesterSupport rsocketRequesterSupport;
    private List<String> p2pServices;
    private int status = 0;

    public UpstreamManagerImpl(RSocketRequesterSupport rsocketRequesterSupport) {
        this.rsocketRequesterSupport = rsocketRequesterSupport;
    }

    @Override
    public void add(UpstreamCluster cluster) {
        clusters.put(cluster.getServiceId(), cluster);
        if (cluster.isBroker()) {
            this.brokerCluster = cluster;
        }
    }

    @Override
    public void remove(UpstreamCluster cluster) {
        this.clusters.remove(cluster.getServiceId());
    }

    public void setP2pServices(List<String> p2pServices) {
        this.p2pServices = p2pServices;
    }

    public List<String> getP2pServices() {
        return p2pServices;
    }

    public void addP2pService(String p2pService) {
        if (this.p2pServices == null) {
            this.p2pServices = new ArrayList<>();
        }
        this.p2pServices.add(p2pService);
    }

    @Override
    public Collection<UpstreamCluster> findAllClusters() {
        return clusters.values();
    }

    @Override
    public UpstreamCluster findClusterByServiceId(String serviceId) {
        return clusters.get(serviceId);
    }

    @Override
    public UpstreamCluster findBroker() {
        return this.brokerCluster;
    }

    @Override
    public DiscoveryService findBrokerDiscoveryService() {
        if (brokerCluster != null && brokerDiscoveryService == null) {
            RSocketRequesterRpcProxy proxy = new RSocketRequesterRpcProxy(this, "",
                    DiscoveryService.class,
                    DiscoveryService.class.getCanonicalName(),
                    "", RSocketMimeType.Hessian, RSocketMimeType.Hessian,
                    Duration.ofMillis(3000), null, false,
                    rsocketRequesterSupport.originUri(), true
            );
            this.brokerDiscoveryService = (DiscoveryService) Proxy.newProxyInstance(
                    DiscoveryService.class.getClassLoader(),
                    new Class[]{DiscoveryService.class},
                    proxy);
        }
        return brokerDiscoveryService;
    }

    @Override
    public RSocket getRSocket(String serviceId) {
        return clusters.getOrDefault(serviceId, brokerCluster).getLoadBalancedRSocket();
    }

    @Override
    public RSocketRequesterSupport requesterSupport() {
        return this.rsocketRequesterSupport;
    }

    @Override
    public void refresh(String serviceId, List<String> uris) {
        clusters.get(serviceId).setUris(uris);
    }

    @Override
    public void init() throws Exception {
        if (!clusters.isEmpty()) {
            //init broker first
            brokerCluster.setRsocketAware(rsocketRequesterSupport);
            brokerCluster.init();
            //init upstream cluster
            for (UpstreamCluster cluster : clusters.values()) {
                if (!cluster.isBroker()) {
                    cluster.setRsocketAware(rsocketRequesterSupport);
                    cluster.init();
                }
            }
        }
        monitorClusters();
        status = 1;
    }

    @Override
    public void close() {
        for (UpstreamCluster cluster : clusters.values()) {
            try {
                cluster.close();
                log.info(RsocketErrorCode.message("RST-400002", cluster.getServiceId()));
            } catch (Exception e) {
                log.error(RsocketErrorCode.message("RST-400001"), e);
            }
        }
    }

    public void monitorClusters() {
        if (status == 0 && brokerCluster != null) {
            if (!brokerCluster.isLocal()) {
                //interval sync to broker to get last broker list in case of UpstreamClusterChangedEvent lost
                Flux.interval(Duration.ofSeconds(120))
                        .flatMap(timestamp -> findBrokerDiscoveryService().getInstances("*"))
                        .map(serviceInstances -> serviceInstances.stream().map(RSocketServiceInstance::getUri).collect(Collectors.toList()))
                        .subscribe(uris -> brokerCluster.setUris(uris));
            }
            if (p2pServices != null && !p2pServices.isEmpty()) {
                // interval sync to p2p service instances list
                Flux.interval(Duration.ofSeconds(120))
                        .flatMap(timestamp -> {
                            return Flux.fromIterable(p2pServices).flatMap(serviceId -> {
                                return findBrokerDiscoveryService().getInstances(serviceId).map(serviceInstances -> {
                                    List<String> uris = serviceInstances.stream().map(RSocketServiceInstance::getUri).collect(Collectors.toList());
                                    return Tuples.of(serviceId, uris);
                                });
                            });
                        })
                        .subscribe(tuple2 -> {
                            List<String> uris = tuple2.getT2();
                            if (!uris.isEmpty()) {
                                UpstreamCluster upstreamCluster = this.clusters.get(tuple2.getT1());
                                if (upstreamCluster != null) {
                                    upstreamCluster.setUris(uris);
                                }
                            }
                        });
            }
        }
    }

}
