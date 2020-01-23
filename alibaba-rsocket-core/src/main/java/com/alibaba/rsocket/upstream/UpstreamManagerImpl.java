package com.alibaba.rsocket.upstream;

import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.rsocket.RSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * upstream manager implementation
 *
 * @author leijuan
 */
public class UpstreamManagerImpl implements UpstreamManager {
    private Logger log = LoggerFactory.getLogger(UpstreamManagerImpl.class);
    private Map<String, UpstreamCluster> clusters = new HashMap<>();
    private UpstreamCluster brokerCluster;
    private RSocketRequesterSupport rsocketRequesterSupport;

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
    public RSocket getRSocket(String serviceId) {
        return clusters.get(serviceId).getLoadBalancedRSocket();
    }

    @Override
    public void refresh(String serviceId, List<String> uris) {
        clusters.get(serviceId).setUris(uris);
    }

    @Override
    public void init() throws Exception {
        if (!clusters.isEmpty()) {
            for (UpstreamCluster cluster : clusters.values()) {
                //rsocket broker cluster
                if (cluster.isBroker()) {
                    //RSocket handler for request from broker
                    AppMetadata appMetadata = new AppMetadata();
                    appMetadata.setName("RSocket Broker");
                    appMetadata.setUuid(UUID.randomUUID().toString());
                }
                cluster.setRsocketAware(rsocketRequesterSupport);
                cluster.init();
            }
        }
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

}
