package com.alibaba.spring.boot.rsocket.broker.route.impl;

import com.alibaba.spring.boot.rsocket.broker.security.RSocketAppPrincipal;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * rsocket app principal mock
 *
 * @author leijuan
 */
public class RSocketAppPrincipalMock implements RSocketAppPrincipal {
    private String name;
    private Set<String> orgs;
    private Set<String> serviceAccounts;

    public RSocketAppPrincipalMock(String name, Set<String> orgs, Set<String> serviceAccounts) {
        this.name = name;
        this.orgs = orgs;
        this.serviceAccounts = serviceAccounts;
    }

    @Override
    public String getSubject() {
        return this.name;
    }

    @Override
    public List<String> getAudience() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Set<String> getRoles() {
        return Collections.EMPTY_SET;
    }

    @Override
    public Set<String> getServiceAccounts() {
        return this.serviceAccounts;
    }

    @Override
    public Set<String> getOrganizations() {
        return this.orgs;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
