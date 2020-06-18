package com.alibaba.spring.boot.rsocket.broker.upstream;

import com.alibaba.rsocket.loadbalance.LoadBalancedRSocket;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.ReplayProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * upstream broker cluster
 *
 * @author leijuan
 */
public class UpstreamBrokerCluster {
    private static Logger log = LoggerFactory.getLogger(UpstreamBrokerCluster.class);
    private List<String> uris = new ArrayList<>();
    /**
     * upstream uris  processor
     */
    private ReplayProcessor<Collection<String>> urisProcessor = ReplayProcessor.cacheLast();
    /**
     * load balanced RSocket to connect service provider or broker instances
     */
    private LoadBalancedRSocket loadBalancedRSocket;
    private RSocketRequesterBySubBroker rsocketRequesterSupport;
    private Integer status = 0;

    public UpstreamBrokerCluster(RSocketRequesterBySubBroker rsocketRequesterSupport) {
        this.rsocketRequesterSupport = rsocketRequesterSupport;
        this.rsocketRequesterSupport.setUpstreamBrokerCluster(this);
    }

    public LoadBalancedRSocket getLoadBalancedRSocket() {
        return loadBalancedRSocket;
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

    private void freshUpstreams() {
        urisProcessor.onNext(uris);
    }

    public void close() {
        try {
            loadBalancedRSocket.dispose();
            log.info(RsocketErrorCode.message("RST-400201"));
        } catch (Exception ignore) {

        }
        status = -1;
    }

    public void init() {
        if (status != 1) {
            //todo customized requester support
            loadBalancedRSocket = new LoadBalancedRSocket("*", urisProcessor, rsocketRequesterSupport);
            freshUpstreams();
            status = 1;
        }
    }
}
