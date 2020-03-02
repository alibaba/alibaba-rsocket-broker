package com.alibaba.spring.boot.rsocket.broker.responder;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.events.CloudEventSupport;
import com.alibaba.rsocket.events.ConfigEvent;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.spring.boot.rsocket.broker.services.ConfigurationService;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.Disposable;
import reactor.extra.processor.TopicProcessor;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * App status cloud event processor
 *
 * @author leijuan
 */
public class AppStatusCloudEventProcessor {
    private Logger log = LoggerFactory.getLogger(AppStatusCloudEventProcessor.class);
    @Autowired
    @Qualifier("reactiveCloudEventProcessor")
    private TopicProcessor<CloudEventImpl> eventProcessor;
    @Autowired
    private RSocketBrokerHandlerRegistry rsocketBrokerHandlerRegistry;
    @Autowired
    private ConfigurationService configurationService;
    private Map<String, Disposable> listeners = new HashMap<>();

    public void init() {
        eventProcessor.subscribe(cloudEvent -> {
            String type = cloudEvent.getAttributes().getType();
            if (AppStatusEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
                handleAppStatusEvent(cloudEvent);
            } else if (ServicesExposedEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
                handleServicesExposedEvent(cloudEvent);
            }
        });
    }

    public void handleAppStatusEvent(CloudEventImpl<?> cloudEvent) {
        AppStatusEvent appStatusEvent = CloudEventSupport.unwrapData(cloudEvent, AppStatusEvent.class);
        //安全验证，确保appStatusEvent的ID和cloud source来源的id一致
        if (appStatusEvent != null && appStatusEvent.getId().equals(cloudEvent.getAttributes().getSource().getHost())) {
            RSocketBrokerResponderHandler responderHandler = rsocketBrokerHandlerRegistry.findByUUID(appStatusEvent.getId());
            if (responderHandler != null) {
                AppMetadata appMetadata = responderHandler.getAppMetadata();
                if (appStatusEvent.getStatus().equals(AppStatusEvent.STATUS_CONNECTED)) {  //app connected
                    registerConfigPush(appMetadata);
                } else if (appStatusEvent.getStatus().equals(AppStatusEvent.STATUS_SERVING)) {  //app serving
                    responderHandler.registerPublishedServices();
                } else if (appStatusEvent.getStatus().equals(AppStatusEvent.STATUS_OUT_OF_SERVICE)) { //app out of service
                    responderHandler.unRegisterPublishedServices();
                } else if (appStatusEvent.getStatus().equals(AppStatusEvent.STATUS_STOPPED)) {
                    responderHandler.unRegisterPublishedServices();
                    responderHandler.setAppStatus(AppStatusEvent.STATUS_STOPPED);
                }
            }
        }
    }

    private void registerConfigPush(AppMetadata appMetadata) {
        String appName = appMetadata.getName();
        if (!listeners.containsKey(appName)) {
            listeners.put(appName, configurationService.watch(appName).subscribe(config -> {
                CloudEventImpl<ConfigEvent> configEvent = CloudEventBuilder.<ConfigEvent>builder()
                        .withId(UUID.randomUUID().toString())
                        .withTime(ZonedDateTime.now())
                        .withSource(URI.create("broker://" + RSocketAppContext.ID))
                        .withType(ConfigEvent.class.getCanonicalName())
                        .withDataContentType("text/x-java-properties")
                        .withData(new ConfigEvent(appName, "text/x-java-properties", config))
                        .build();
                rsocketBrokerHandlerRegistry.broadcast(appName, configEvent).subscribe();
            }));
        }
    }

    public void handleServicesExposedEvent(CloudEventImpl<?> cloudEvent) {
        ServicesExposedEvent servicesExposedEvent = CloudEventSupport.unwrapData(cloudEvent, ServicesExposedEvent.class);
        if (servicesExposedEvent != null && servicesExposedEvent.getAppId().equals(cloudEvent.getAttributes().getSource().getHost())) {
            RSocketBrokerResponderHandler responderHandler = rsocketBrokerHandlerRegistry.findByUUID(servicesExposedEvent.getAppId());
            if (responderHandler != null) {
                responderHandler.setPeerServices(servicesExposedEvent.getServices());
                responderHandler.registerPublishedServices();
            }
        }
    }
}
