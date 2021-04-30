package com.alibaba.rsocket.upstream;

import com.alibaba.rsocket.Initializable;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.loadbalance.LoadBalancedRSocket;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.ReplayProcessor;

import java.io.Closeable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * upstream cluster for a service with group and version
 *
 * @author leijuan
 */
public class UpstreamCluster implements Initializable, Closeable {
    private Logger log = LoggerFactory.getLogger(UpstreamCluster.class);
    private String group;
    private String serviceName;
    private String version;
    /**
     * last uri list
     */
    private List<String> uris = new ArrayList<>();
    /**
     * upstream uris  processor
     */
    private ReplayProcessor<Collection<String>> urisProcessor = ReplayProcessor.cacheLast();
    /**
     * load balanced RSocket to connect service provider or broker instances
     */
    private LoadBalancedRSocket loadBalancedRSocket;
    /**
     * app rsocket aware
     */
    private RSocketRequesterSupport rsocketRequesterSupport;
    /**
     * upstream cluster: 0: ready , 1: connected, -1: disposed
     */
    private Integer status = 0;

    public UpstreamCluster(String group, String serviceName, String version) {
        this.group = group;
        this.serviceName = serviceName;
        this.version = version;
    }

    public UpstreamCluster(String group, String serviceName, String version, List<String> uris) {
        this(group, serviceName, version);
        this.setUris(uris);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getVersion() {
        return version;
    }

    public String getGroup() {
        return group;
    }

    public Integer getStatus() {
        return status;
    }

    public String getServiceId() {
        return ServiceLocator.serviceId(group, serviceName, version);
    }

    public void setUris(List<String> uris) {
        //validate the uris same or not
        if (uris.size() == this.uris.size() && this.uris.containsAll(uris)) {
            return;
        }
        this.uris = uris;
        //lazy fresh after init
        if (status == 1) {
            freshUpstreams();
        }
    }


    public List<String> getUris() {
        return uris;
    }

    public void setRsocketAware(RSocketRequesterSupport rsocketRequesterSupport) {
        this.rsocketRequesterSupport = rsocketRequesterSupport;
    }

    private void freshUpstreams() {
        urisProcessor.onNext(uris);
    }

    public boolean isBroker() {
        return serviceName.equals("*");
    }

    public LoadBalancedRSocket getLoadBalancedRSocket() {
        return loadBalancedRSocket;
    }

    public boolean isLocal() {
        if (uris != null && uris.size() == 1) {
            String host = URI.create(uris.get(0)).getHost();
            return host.equals("127.0.0.1") || host.equals("localhost");
        }
        return false;
    }

    @Override
    public void close() {
        try {
            loadBalancedRSocket.dispose();
            log.info(RsocketErrorCode.message("RST-400201"));
        } catch (Exception ignore) {

        }
        status = -1;
    }

    @Override
    public void init() {
        if (status != 1) {
            if (!this.uris.isEmpty()) {
                freshUpstreams();
            }
            loadBalancedRSocket = new LoadBalancedRSocket(getServiceId(), urisProcessor, rsocketRequesterSupport);
            status = 1;
        }
    }

}
