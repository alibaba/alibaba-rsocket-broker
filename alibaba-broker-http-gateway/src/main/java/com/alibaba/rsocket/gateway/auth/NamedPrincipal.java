package com.alibaba.rsocket.gateway.auth;

import java.security.Principal;

/**
 * named Principal
 *
 * @author leijuan
 */
public class NamedPrincipal implements Principal {
    private String name;

    public NamedPrincipal() {

    }

    public NamedPrincipal(String name) {
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
