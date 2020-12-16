package com.alibaba.rsocket.events;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * services exposed event: register service on routing table
 *
 * @author leijuan
 */
public class ServicesExposedEvent implements CloudEventSupport<ServicesExposedEvent> {
    /**
     * app UUID
     */
    private String appId;
    /**
     * exposed services
     */
    private Set<ServiceLocator> services = new HashSet<>();

    public ServicesExposedEvent() {
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Set<ServiceLocator> getServices() {
        return services;
    }

    public void setServices(Set<ServiceLocator> services) {
        this.services = services;
    }

    public void addService(ServiceLocator serviceLocator) {
        this.services.add(serviceLocator);
    }

    public static CloudEventImpl<ServicesExposedEvent> convertServicesToCloudEvent(Collection<ServiceLocator> serviceLocators) {
        ServicesExposedEvent servicesExposedEvent = new ServicesExposedEvent();
        for (ServiceLocator serviceLocator : serviceLocators) {
            servicesExposedEvent.addService(serviceLocator);
        }
        servicesExposedEvent.setAppId(RSocketAppContext.ID);
        return RSocketCloudEventBuilder
                .builder(servicesExposedEvent)
                .build();
    }
}
