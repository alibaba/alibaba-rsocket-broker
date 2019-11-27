package com.alibaba.rsocket;

import com.alibaba.rsocket.utils.MurmurHash3;

import java.util.Objects;

/**
 * service locator: group, service full name, version and lables
 *
 * @author leijuan
 */
public class ServiceLocator {
    private String group;
    private String service;
    private String version;
    private String[] labels;

    public ServiceLocator() {
    }

    public ServiceLocator(String group, String service, String version) {
        this.group = group;
        this.service = service;
        this.version = version;
    }

    public ServiceLocator(String group, String service, String version, String[] labels) {
        this(group, service, version);
        this.labels = labels;
    }

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

    public String[] getLabels() {
        return labels;
    }

    public void setLabels(String[] labels) {
        this.labels = labels;
    }

    public String routing() {
        return serviceId(group, service, version);
    }

    public Integer id() {
        return MurmurHash3.hash32(routing());
    }

    @Override
    public String toString() {
        return serviceId(this.group, this.service, this.version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceLocator that = (ServiceLocator) o;
        return Objects.equals(group, that.group) &&
                Objects.equals(service, that.service) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, service, version);
    }

    public static String serviceId(String group, String service, String version) {
        String serviceId = service;
        if (group != null && !group.isEmpty()) {
            serviceId = group + ":" + serviceId;
        }
        if (version != null && !version.isEmpty()) {
            serviceId = serviceId + ":" + version;
        }
        return serviceId;
    }
}
