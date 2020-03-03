package com.alibaba.spring.boot.rsocket.responder;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.loadbalance.LoadBalancedRSocket;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamManager;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import io.rsocket.metadata.WellKnownMimeType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RSocket services publish hook
 *
 * @author leijuan
 */
public class RSocketServicesPublishHook implements ApplicationListener<ApplicationReadyEvent> {
    private static Logger log = LoggerFactory.getLogger(RSocketServicesPublishHook.class);
    @Autowired
    private UpstreamManager upstreamManager;
    @Autowired
    private RSocketRequesterSupport rsocketRequesterSupport;

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent applicationReadyEvent) {
        UpstreamCluster brokerCluster = upstreamManager.findBroker();
        if (brokerCluster == null) return;
        //rsocket broker cluster logic
        CloudEventImpl<AppStatusEvent> appStatusEventCloudEvent = CloudEventBuilder.<AppStatusEvent>builder()
                .withId(UUID.randomUUID().toString())
                .withTime(ZonedDateTime.now())
                .withSource(URI.create("app://" + RSocketAppContext.ID))
                .withType(AppStatusEvent.class.getCanonicalName())
                .withDataContentType(WellKnownMimeType.APPLICATION_JSON.getString())
                .withData(new AppStatusEvent(RSocketAppContext.ID, AppStatusEvent.STATUS_SERVING))
                .build();
        LoadBalancedRSocket loadBalancedRSocket = brokerCluster.getLoadBalancedRSocket();
        String brokers = String.join(",", loadBalancedRSocket.getActiveSockets().keySet());
        loadBalancedRSocket.fireCloudEventToUpstreamAll(appStatusEventCloudEvent)
                .doOnSuccess(aVoid -> log.info(RsocketErrorCode.message("RST-301200", brokers)))
                .subscribe();
        CloudEventImpl<ServicesExposedEvent> servicesExposedEventCloudEvent = rsocketRequesterSupport.servicesExposedEvent().get();
        if (servicesExposedEventCloudEvent != null) {
            loadBalancedRSocket.fireCloudEventToUpstreamAll(servicesExposedEventCloudEvent).doOnSuccess(aVoid -> {
                String exposedServices = rsocketRequesterSupport.exposedServices().get().stream().map(ServiceLocator::getGsv).collect(Collectors.joining(","));
                log.info(RsocketErrorCode.message("RST-301201", exposedServices, brokers));
            }).subscribe();
        }
    }
}
