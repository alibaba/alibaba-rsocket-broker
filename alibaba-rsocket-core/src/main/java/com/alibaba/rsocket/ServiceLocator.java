package com.alibaba.rsocket;

import com.alibaba.rsocket.utils.MurmurHash3;

import java.util.Objects;

/**
 * service locator: group, service full name, version and tags
 *
 * @author leijuan
 */
public class ServiceLocator {
    private String group;
    private String service;
    private String version;
    private String[] tags;
    private String gsv;
    private Integer id;

    public ServiceLocator() {
    }

    public ServiceLocator(String group, String service, String version) {
        this.group = group;
        this.service = service;
        this.version = version;
        this.gsv = serviceId(group, service, version);
        this.id = MurmurHash3.hash32(this.gsv);
    }

    public ServiceLocator(String group, String service, String version, String[] tags) {
        this(group, service, version);
        this.tags = tags;
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

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public String getGsv() {
        return this.gsv;
    }

    public Integer getId() {
        return this.id;
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
        return this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, service, version);
    }

    public static String serviceId(String group, String service, String version) {
        StringBuilder routingBuilder = new StringBuilder();
        //group
        if (group != null && !group.isEmpty()) {
            routingBuilder.append(group).append("!");
        }
        //service
        routingBuilder.append(service);
        //version
        if (version != null && !version.isEmpty()) {
            routingBuilder.append(":").append(version);
        }
        return routingBuilder.toString();
    }

    public static Integer serviceHashCode(String routingKey) {
        return MurmurHash3.hash32(routingKey);
    }
}
