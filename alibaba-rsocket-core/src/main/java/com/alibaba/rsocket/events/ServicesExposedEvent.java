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
    private String appId;
    private Set<ServiceLocator> published = new HashSet<>();

    public ServicesExposedEvent() {
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Set<ServiceLocator> getPublished() {
        return published;
    }

    public void setPublished(Set<ServiceLocator> published) {
        this.published = published;
    }

    public void addService(ServiceLocator serviceLocator) {
        this.published.add(serviceLocator);
    }
}
