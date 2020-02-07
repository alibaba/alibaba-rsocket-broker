package com.alibaba.rsocket.events;

import com.alibaba.rsocket.ServiceLocator;

import java.util.HashSet;
import java.util.Set;

/**
 * services exposed event
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
}
