package com.alibaba.spring.boot.rsocket.broker.route.impl;

import com.alibaba.rsocket.utils.MurmurHash3;
import com.alibaba.spring.boot.rsocket.broker.security.RSocketAppPrincipal;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * rsocket app principal mock
 *
 * @author leijuan
 */
public class RSocketAppPrincipalMock implements RSocketAppPrincipal {
    private String tokenId;
    private Integer hashcode;
    private String name;
    private Set<String> orgs;
    private Set<String> serviceAccounts;

    public RSocketAppPrincipalMock(String name, Set<String> orgs, Set<String> serviceAccounts) {
        this.name = name;
        this.orgs = orgs;
        this.serviceAccounts = serviceAccounts;
        this.hashcode = MurmurHash3.hash32(name + ":" + String.join(",", orgs) + ":" + String.join(",", serviceAccounts));
        this.tokenId = UUID.randomUUID().toString();
    }

    @Override
    public String getTokenId() {
        return this.tokenId;
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
    public Set<String> getAuthorities() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RSocketAppPrincipalMock that = (RSocketAppPrincipalMock) o;
        return this.hashcode.equals(that.hashcode);
    }

    @Override
    public int hashCode() {
        return hashcode;
    }
}
