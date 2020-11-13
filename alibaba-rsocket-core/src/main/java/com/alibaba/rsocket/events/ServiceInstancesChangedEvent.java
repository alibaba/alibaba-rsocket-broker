package com.alibaba.rsocket.events;

import java.util.List;

/**
 * service instances changed event
 *
 * @author leijuan
 */
public class ServiceInstancesChangedEvent implements CloudEventSupport<ServiceInstancesChangedEvent> {
    private String group;
    private String service;
    private String version;
    /**
     * type for changing: -1: removed, 1: added, 0: replaced
     */
    private Integer type;
    private List<String> uris;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }
}
