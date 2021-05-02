package com.alibaba.spring.boot.rsocket.broker.responder;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.events.*;
import com.alibaba.rsocket.metadata.AppMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Sinks;

import java.util.Set;

/**
 * App status cloud event processor
 *
 * @author leijuan
 */
public class AppStatusCloudEventProcessor {
    @SuppressWarnings("rawtypes")
    @Autowired
    @Qualifier("reactiveCloudEventProcessor")
    private Sinks.Many<CloudEventImpl> eventProcessor;
    @Autowired
    private RSocketBrokerHandlerRegistry rsocketBrokerHandlerRegistry;

    public void init() {
        eventProcessor.asFlux().subscribe(cloudEvent -> {
            String type = cloudEvent.getAttributes().getType();
            if (AppStatusEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
                handleAppStatusEvent(cloudEvent);
            } else if (PortsUpdateEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
                handlerPortsUpdateEvent(cloudEvent);
            } else if (ServicesExposedEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
                handleServicesExposedEvent(cloudEvent);
            } else if (ServicesHiddenEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
                handleServicesHiddenEvent(cloudEvent);
            }
        });
    }

    public void handleAppStatusEvent(CloudEventImpl<?> cloudEvent) {
        AppStatusEvent appStatusEvent = CloudEventSupport.unwrapData(cloudEvent, AppStatusEvent.class);
        //安全验证，确保appStatusEvent的ID和cloud source来源的id一致
        if (appStatusEvent != null && appStatusEvent.getId().equals(cloudEvent.getAttributes().getSource().getHost())) {
            RSocketBrokerResponderHandler responderHandler = rsocketBrokerHandlerRegistry.findByUUID(appStatusEvent.getId());
            if (responderHandler != null) {
                if (appStatusEvent.getStatus().equals(AppStatusEvent.STATUS_SERVING)) {  //app serving
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

    public void handleServicesExposedEvent(CloudEventImpl<?> cloudEvent) {
        ServicesExposedEvent servicesExposedEvent = CloudEventSupport.unwrapData(cloudEvent, ServicesExposedEvent.class);
        if (servicesExposedEvent != null && servicesExposedEvent.getAppId().equals(cloudEvent.getAttributes().getSource().getHost())) {
            RSocketBrokerResponderHandler responderHandler = rsocketBrokerHandlerRegistry.findByUUID(servicesExposedEvent.getAppId());
            if (responderHandler != null) {
                Set<ServiceLocator> services = servicesExposedEvent.getServices();
                responderHandler.setAppStatus(AppStatusEvent.STATUS_SERVING);
                responderHandler.registerServices(services);
            }
        }
    }

    public void handleServicesHiddenEvent(CloudEventImpl<?> cloudEvent) {
        ServicesHiddenEvent servicesHiddenEvent = CloudEventSupport.unwrapData(cloudEvent, ServicesHiddenEvent.class);
        if (servicesHiddenEvent != null && servicesHiddenEvent.getAppId().equals(cloudEvent.getAttributes().getSource().getHost())) {
            RSocketBrokerResponderHandler responderHandler = rsocketBrokerHandlerRegistry.findByUUID(servicesHiddenEvent.getAppId());
            if (responderHandler != null) {
                Set<ServiceLocator> services = servicesHiddenEvent.getServices();
                responderHandler.unRegisterServices(services);
            }
        }
    }

    public void handlerPortsUpdateEvent(CloudEventImpl<?> cloudEvent) {
        PortsUpdateEvent portsUpdateEvent = CloudEventSupport.unwrapData(cloudEvent, PortsUpdateEvent.class);
        if (portsUpdateEvent != null) {
            RSocketBrokerResponderHandler responderHandler = rsocketBrokerHandlerRegistry.findByUUID(portsUpdateEvent.getAppId());
            if (responderHandler != null) {
                AppMetadata appMetadata = responderHandler.getAppMetadata();
                appMetadata.setWebPort(portsUpdateEvent.getWebPort());
                appMetadata.setManagementPort(portsUpdateEvent.getManagementPort());
                appMetadata.setRsocketPorts(portsUpdateEvent.getRsocketPorts());
            }
        }
    }
}
