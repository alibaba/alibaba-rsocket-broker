package com.alibaba.spring.boot.rsocket.broker.security;

import java.security.Principal;
import java.util.List;
import java.util.Set;

/**
 * RSocket APP Principal: 接入认证信息
 *
 * @author leijuan
 */
public interface RSocketAppPrincipal extends Principal {
    /**
     * get token id
     *
     * @return token id
     */
    String getTokenId();

    String getSubject();

    List<String> getAudience();

    Set<String> getRoles();

    Set<String> getAuthorities();

    Set<String> getServiceAccounts();

    Set<String> getOrganizations();

}
