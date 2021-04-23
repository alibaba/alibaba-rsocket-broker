package com.alibaba.rsocket.upstream;

import com.alibaba.rsocket.Initializable;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.discovery.DiscoveryService;
import io.rsocket.RSocket;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;

/**
 * upstream manager
 *
 * @author leijuan
 */
public interface UpstreamManager extends Initializable, Closeable {

    void add(UpstreamCluster cluster);

    Collection<UpstreamCluster> findAllClusters();

    UpstreamCluster findClusterByServiceId(String serviceId);

    UpstreamCluster findBroker();

    DiscoveryService findBrokerDiscoveryService();

    /**
     * get rsocket for service id with load balance support
     *
     * @param serviceId service id
     * @return rsocket
     */
    RSocket getRSocket(String serviceId);

    RSocketRequesterSupport requesterSupport();
    /**
     * refresh service  with new uri list
     *
     * @param serviceId service id
     * @param uris      uri list
     */
    void refresh(String serviceId, List<String> uris);

    void init() throws Exception;

    void close();
}
