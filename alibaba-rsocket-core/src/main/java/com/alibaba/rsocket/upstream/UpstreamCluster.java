package com.alibaba.rsocket.upstream;

import com.alibaba.rsocket.Initializable;
import com.alibaba.rsocket.PayloadUtils;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventRSocket;
import com.alibaba.rsocket.loadbalance.LoadBalancedRSocket;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.cloudevents.CloudEvent;
import io.cloudevents.v1.CloudEventImpl;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * upstream cluster and add timeout
 *
 * @author leijuan
 */
public class UpstreamCluster extends AbstractRSocket implements CloudEventRSocket, Initializable, Closeable {
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
     * load balanced RSocket: load balance and auto reconnect
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

    public LoadBalancedRSocket getLoadBalancedRSocketMono() {
        return loadBalancedRSocket;
    }

    //------------- RSocket methods Start------------//

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        return loadBalancedRSocket.requestResponse(payload);
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
        return loadBalancedRSocket.fireAndForget(payload);
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
        return loadBalancedRSocket.requestStream(payload);
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return loadBalancedRSocket.requestChannel(payloads);
    }

    @Override
    public Mono<Void> fireCloudEvent(CloudEventImpl<?> cloudEvent) {
        try {
            return metadataPush(PayloadUtils.cloudEventToMetadataPushPayload(cloudEvent));
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    @Override
    public Mono<Void> metadataPush(Payload payload) {
        return loadBalancedRSocket.metadataPush(payload);
    }

    @Override
    public double availability() {
        return loadBalancedRSocket.availability();
    }

    @Override
    public void dispose() {
        super.dispose();
        close();
    }

    @Override
    public boolean isDisposed() {
        return super.isDisposed();
    }

    @Override
    public Mono<Void> onClose() {
        return super.onClose();
    }

    //------------- RSocket methods End------------//

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
            loadBalancedRSocket = new LoadBalancedRSocket(getServiceId(), urisProcessor, rsocketRequesterSupport);
            freshUpstreams();
            status = 1;
        }
    }

    /**
     * fire cloud event to upstream all nodes
     *
     * @param cloudEvent cloud event
     * @return void
     */
    public Mono<Void> fireCloudEventToUpstreamAll(CloudEventImpl<?> cloudEvent) {
        try {
            Payload payload = PayloadUtils.cloudEventToMetadataPushPayload(cloudEvent);
            return Flux.fromIterable(loadBalancedRSocket.getActiveSockets().values())
                    .flatMap(rSocket -> rSocket.metadataPush(payload))
                    .doOnError(throwable -> log.error("Failed to fire event to upstream", throwable))
                    .then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
