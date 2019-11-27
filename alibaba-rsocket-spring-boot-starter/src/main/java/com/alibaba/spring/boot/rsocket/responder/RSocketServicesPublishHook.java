package com.alibaba.spring.boot.rsocket.responder;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.spring.boot.rsocket.RSocketProperties;
import io.cloudevents.CloudEvent;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
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
    private Logger log = LoggerFactory.getLogger(RSocketServicesPublishHook.class);
    @Autowired
    private ApplicationContext appContext;
    @Autowired
    private UpstreamManager upstreamManager;
    @Autowired
    private RSocketProperties rsocketProperties;
    @Autowired
    private RSocketRequesterSupport rsocketRequesterSupport;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        UpstreamCluster brokerCluster = upstreamManager.findClusterByServiceId("*");
        if (brokerCluster == null) return;
        //rsocket broker cluster logic
        CloudEventImpl<AppStatusEvent> appStatusEventCloudEvent = CloudEventBuilder.<AppStatusEvent>builder()
                .withId(UUID.randomUUID().toString())
                .withTime(ZonedDateTime.now())
                .withSource(URI.create("app://" + RSocketAppContext.ID))
                .withType(AppStatusEvent.class.getCanonicalName())
                .withDataContentType("application/json")
                .withData(new AppStatusEvent(RSocketAppContext.ID, AppStatusEvent.STATUS_SERVING))
                .build();
        brokerCluster.fireCloudEventToUpstreamAll(appStatusEventCloudEvent).doOnSuccess(aVoid -> log.info("App status: Serving")).subscribe();
        CloudEventImpl<ServicesExposedEvent> servicesExposedEventCloudEvent = rsocketRequesterSupport.servicesExposedEvent().get();
        if (servicesExposedEventCloudEvent != null) {
            brokerCluster.fireCloudEventToUpstreamAll(servicesExposedEventCloudEvent).doOnSuccess(aVoid -> {
                log.info("Services exposed on Broker: " + rsocketRequesterSupport.exposedServices().get().stream().map(ServiceLocator::toString).collect(Collectors.joining(",")));
            }).subscribe();
        }
    }
}
