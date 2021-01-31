package com.alibaba.rsocket.client;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.events.CloudEventsProcessor;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.events.ServicesHiddenEvent;
import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamClusterChangedEventConsumer;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.rsocket.upstream.UpstreamManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.extra.processor.TopicProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RSocket Broker Client
 *
 * @author leijuan
 */
@SuppressWarnings("rawtypes")
public class RSocketBrokerClient {
    private static Logger log = LoggerFactory.getLogger(RSocketBrokerClient.class);
    private List<String> brokers;
    private String appName;
    private RSocketMimeType dataMimeType;
    private UpstreamManager upstreamManager;
    private LocalReactiveServiceCaller serviceCaller;
    private TopicProcessor<CloudEventImpl> eventProcessor;
    private SimpleRSocketRequesterSupport rsocketRequesterSupport;
    private CloudEventsProcessor cloudEventsProcessor;

    public RSocketBrokerClient(String appName, List<String> brokers,
                               RSocketMimeType dataMimeType, char[] jwtToken,
                               LocalReactiveServiceCaller serviceCaller) {
        this.appName = appName;
        this.brokers = brokers;
        this.dataMimeType = dataMimeType;
        this.eventProcessor = TopicProcessor.<CloudEventImpl>builder().name("cloud-events-processor").build();
        this.serviceCaller = serviceCaller;
        // add health check
        this.serviceCaller.addProvider("", RSocketServiceHealth.class.getCanonicalName(), "",
                RSocketServiceHealth.class, (RSocketServiceHealth) serviceName -> Mono.just(1));
        this.rsocketRequesterSupport = new SimpleRSocketRequesterSupport(appName, jwtToken, this.brokers,
                this.serviceCaller, this.eventProcessor);
        this.cloudEventsProcessor = new CloudEventsProcessor(eventProcessor, new ArrayList<>());
        initUpstreamManager();
    }

    private void initUpstreamManager() {
        this.upstreamManager = new UpstreamManagerImpl(rsocketRequesterSupport);
        upstreamManager.add(new UpstreamCluster(null, "*", null, this.brokers));
        try {
            upstreamManager.init();
            this.cloudEventsProcessor.addConsumer(new UpstreamClusterChangedEventConsumer(upstreamManager));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RSocketBrokerClient addService(String serviceName, Class<?> serviceInterface, Object handler) {
        this.serviceCaller.addProvider("", serviceName, "", serviceInterface, handler);
        return this;
    }

    public void publishServices() {
        CloudEventImpl<ServicesExposedEvent> servicesExposedEventCloudEvent = rsocketRequesterSupport.servicesExposedEvent().get();
        if (servicesExposedEventCloudEvent != null) {
            upstreamManager.findBroker().getLoadBalancedRSocket().fireCloudEventToUpstreamAll(servicesExposedEventCloudEvent).doOnSuccess(aVoid -> {
                String exposedServices = rsocketRequesterSupport.exposedServices().get().stream().map(ServiceLocator::getGsv).collect(Collectors.joining(","));
                log.info(RsocketErrorCode.message("RST-301201", exposedServices, brokers));
            }).subscribe();
        }
    }

    public void removeService(String serviceName, Class<?> serviceInterface) {
        ServiceLocator targetService = new ServiceLocator("", serviceName, "");
        CloudEventImpl<ServicesHiddenEvent> cloudEvent = ServicesHiddenEvent.convertServicesToCloudEvent(Collections.singletonList(targetService));
        upstreamManager.findBroker().getLoadBalancedRSocket().fireCloudEventToUpstreamAll(cloudEvent)
                .doOnSuccess(unused -> {
                    this.serviceCaller.removeProvider("", serviceName, "", serviceInterface);
                }).subscribe();
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
