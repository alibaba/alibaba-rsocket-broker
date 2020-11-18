package com.alibaba.rsocket.client;

import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamClusterChangedEventProcessor;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.rsocket.upstream.UpstreamManagerImpl;
import io.cloudevents.v1.CloudEventImpl;
import reactor.extra.processor.TopicProcessor;

import java.util.List;

/**
 * RSocket Broker Client
 *
 * @author leijuan
 */
@SuppressWarnings("rawtypes")
public class RSocketBrokerClient {
    private char[] jwtToken;
    private List<String> brokers;
    private String appName;
    private RSocketMimeType dataMimeType;
    private UpstreamManager upstreamManager;
    private LocalReactiveServiceCaller serviceCaller;
    private TopicProcessor<CloudEventImpl> eventProcessor;

    public RSocketBrokerClient(String appName, List<String> brokers,
                               RSocketMimeType dataMimeType, char[] jwtToken,
                               LocalReactiveServiceCaller serviceCaller) {
        this.jwtToken = jwtToken;
        this.brokers = brokers;
        this.appName = appName;
        this.dataMimeType = dataMimeType;
        this.eventProcessor = TopicProcessor.<CloudEventImpl>builder().name("cloud-events-processor").build();
        this.serviceCaller = serviceCaller;
        initUpstreamManager();
    }

    private void initUpstreamManager() {
        SimpleRSocketRequesterSupport rsocketRequesterSupport = new SimpleRSocketRequesterSupport(this.appName, this.jwtToken, this.brokers,
                this.serviceCaller, this.eventProcessor);
        this.upstreamManager = new UpstreamManagerImpl(rsocketRequesterSupport);
        upstreamManager.add(new UpstreamCluster(null, "*", null, this.brokers));
        try {
            upstreamManager.init();
            new UpstreamClusterChangedEventProcessor(upstreamManager, eventProcessor).init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dispose() {
        this.upstreamManager.close();
    }

    public <T> T buildService(Class<T> serviceInterface) {
        return RSocketRemoteServiceBuilder
                .client(serviceInterface)
                .service(serviceInterface.getCanonicalName())
                .encodingType(this.dataMimeType)
                .acceptEncodingType(this.dataMimeType)
                .upstreamManager(this.upstreamManager)
                .build();
    }

    public <T> T buildService(Class<T> serviceInterface, String serviceName) {
        return RSocketRemoteServiceBuilder
                .client(serviceInterface)
                .service(serviceName)
                .encodingType(this.dataMimeType)
                .acceptEncodingType(this.dataMimeType)
                .upstreamManager(this.upstreamManager)
                .build();
    }

}
