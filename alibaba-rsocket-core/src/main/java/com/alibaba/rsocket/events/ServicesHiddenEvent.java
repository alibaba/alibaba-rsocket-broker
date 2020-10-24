package com.alibaba.rsocket.events;

import com.alibaba.rsocket.ServiceLocator;

import java.util.HashSet;
import java.util.Set;

/**
 * services hidden event: remove services from routing table
 *
 * @author leijuan
 */
public class ServicesHiddenEvent implements CloudEventSupport<ServicesHiddenEvent> {
    /**
     * app UUID
     */
    private String appId;
    /**
     * hidden services
     */
    private Set<ServiceLocator> services = new HashSet<>();

    public ServicesHiddenEvent() {
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
