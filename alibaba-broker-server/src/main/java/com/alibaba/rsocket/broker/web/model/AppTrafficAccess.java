package com.alibaba.rsocket.broker.web.model;

import java.util.List;

/**
 * App traffic access
 *
 * @author leijuan
 */
public class AppTrafficAccess {
    private String appName;
    private String orgs;
    private String serviceAccounts;
    private List<String> internalServices;
    private List<String> grantedServices;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
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

    public List<String> getInternalServices() {
        return internalServices;
    }

    public void setInternalServices(List<String> internalServices) {
        this.internalServices = internalServices;
    }

    public List<String> getGrantedServices() {
        return grantedServices;
    }

    public void setGrantedServices(List<String> grantedServices) {
        this.grantedServices = grantedServices;
    }
}
