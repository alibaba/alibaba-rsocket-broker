package com.alibaba.spring.boot.rsocket.responder;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.events.PortsUpdateEvent;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.loadbalance.LoadBalancedRSocket;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

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
        CloudEventImpl<AppStatusEvent> appStatusEventCloudEvent = RSocketCloudEventBuilder
                .builder(new AppStatusEvent(RSocketAppContext.ID, AppStatusEvent.STATUS_SERVING))
                .build();
        LoadBalancedRSocket loadBalancedRSocket = brokerCluster.getLoadBalancedRSocket();
        String brokers = String.join(",", loadBalancedRSocket.getActiveSockets().keySet());
        //ports update
        ConfigurableEnvironment env = applicationReadyEvent.getApplicationContext().getEnvironment();
        int serverPort = Integer.parseInt(env.getProperty("server.port", "0"));
        if (serverPort == 0) {
            if (RSocketAppContext.webPort > 0 || RSocketAppContext.managementPort > 0 || RSocketAppContext.rsocketPorts != null) {
                PortsUpdateEvent portsUpdateEvent = new PortsUpdateEvent();
                portsUpdateEvent.setAppId(RSocketAppContext.ID);
                portsUpdateEvent.setWebPort(RSocketAppContext.webPort);
                portsUpdateEvent.setManagementPort(RSocketAppContext.managementPort);
                portsUpdateEvent.setRsocketPorts(RSocketAppContext.rsocketPorts);
                CloudEventImpl<PortsUpdateEvent> portsUpdateCloudEvent = RSocketCloudEventBuilder
                        .builder(portsUpdateEvent)
                        .build();
                loadBalancedRSocket.fireCloudEventToUpstreamAll(portsUpdateCloudEvent)
                        .doOnSuccess(aVoid -> log.info(RsocketErrorCode.message("RST-301200", brokers)))
                        .subscribe();
            }
        }
        // app status
        loadBalancedRSocket.fireCloudEventToUpstreamAll(appStatusEventCloudEvent)
                .doOnSuccess(aVoid -> log.info(RsocketErrorCode.message("RST-301200", brokers)))
                .subscribe();
        // service exposed
        CloudEventImpl<ServicesExposedEvent> servicesExposedEventCloudEvent = rsocketRequesterSupport.servicesExposedEvent().get();
        if (servicesExposedEventCloudEvent != null) {
            loadBalancedRSocket.fireCloudEventToUpstreamAll(servicesExposedEventCloudEvent).doOnSuccess(aVoid -> {
                String exposedServices = rsocketRequesterSupport.exposedServices().get().stream().map(ServiceLocator::getGsv).collect(Collectors.joining(","));
                log.info(RsocketErrorCode.message("RST-301201", exposedServices, brokers));
            }).subscribe();
        }
    }
}
