package com.alibaba.rsocket.broker.web.model;

/**
 * Service Info
 *
 * @author leijuan
 */
public class ServiceInfo {
    private String group;
    private String service;
    private String version;
    private Long counter;
    private int instances;
    private String orgs;
    private String serviceAccounts;

    public ServiceInfo() {
    }

    public ServiceInfo(String group, String service, String version, Long counter, int instances) {
        this.group = group;
        this.service = service;
        this.version = version;
        this.counter = counter;
        this.instances = instances;
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

    public Long getCounter() {
        return counter;
    }

    public void setCounter(Long counter) {
        this.counter = counter;
    }

    public int getInstances() {
        return instances;
    }

    public void setInstances(int instances) {
        this.instances = instances;
    }

    public String getOrgs() {
        return orgs;
    }

    public void setOrgs(String orgs) {
        this.orgs = orgs;
    }

    public String getServiceAccounts() {
        return serviceAccounts;
    }

    public void setServiceAccounts(String serviceAccounts) {
        this.serviceAccounts = serviceAccounts;
    }
}
