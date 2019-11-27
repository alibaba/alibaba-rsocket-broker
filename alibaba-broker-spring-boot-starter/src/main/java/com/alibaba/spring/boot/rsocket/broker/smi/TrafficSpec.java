package com.alibaba.spring.boot.rsocket.broker.smi;

import java.util.Set;

/**
 * Traffic Spec: 避免大量对象生成，采用flight weight pattern
 *
 * @author linux_china
 */
public class TrafficSpec {
    private Set<String> orgs;
    private Set<String> serviceAccounts;
    private String appName;
    private String targetService;

    public Set<String> getOrgs() {
        return orgs;
    }

    public void setOrgs(Set<String> orgs) {
        this.orgs = orgs;
    }

    public Set<String> getServiceAccounts() {
        return serviceAccounts;
    }

    public void setServiceAccounts(Set<String> serviceAccounts) {
        this.serviceAccounts = serviceAccounts;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTargetService() {
        return targetService;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }
}
