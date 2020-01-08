package com.alibaba.spring.boot.rsocket.broker.security;


import com.alibaba.rsocket.utils.MurmurHash3;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import javax.security.auth.Subject;
import java.util.*;

/**
 * principal with JWT backend
 *
 * @author leijuan
 */
public class JwtPrincipal implements RSocketAppPrincipal {
    private Integer hashcode;
    private String subject;
    private List<String> audience;
    private Set<String> roles;
    private Set<String> authorities;
    private Set<String> serviceAccounts;
    private Set<String> organizations;

    public JwtPrincipal() {

    }

    public JwtPrincipal(DecodedJWT decodedJWT, String credentials) {
        this.hashcode = MurmurHash3.hash32(credentials);
        this.subject = decodedJWT.getSubject();
        this.audience = decodedJWT.getAudience();
        Map<String, Claim> claims = decodedJWT.getClaims();
        this.serviceAccounts = new HashSet<>(decodedJWT.getClaim("sas").asList(String.class));
        this.organizations = new HashSet<>(decodedJWT.getClaim("orgs").asList(String.class));
        if (claims.containsKey("roles")) {
            this.roles = new HashSet<>(decodedJWT.getClaim("roles").asList(String.class));
        }
        if (claims.containsKey("authorities")) {
            this.authorities = new HashSet<>(decodedJWT.getClaim("authorities").asList(String.class));
        }
    }

    public JwtPrincipal(String subject, List<String> audience, Set<String> roles, Set<String> authorities, Set<String> serviceAccounts, Set<String> organizations) {
        this.hashcode = MurmurHash3.hash32(subject);
        this.subject = subject;
        this.audience = audience;
        this.roles = roles;
        this.authorities = authorities;
        this.serviceAccounts = serviceAccounts;
        this.organizations = organizations;
    }

    @Override
    public String getName() {
        return subject;
    }

    public String getSubject() {
        return subject;
    }

    public List<String> getAudience() {
        return audience;
    }

    public Set<String> getRoles() {
        return roles == null ? Collections.emptySet() : roles;
    }

    public Set<String> getAuthorities() {
        return authorities == null ? Collections.emptySet() : authorities;
    }

    public Set<String> getServiceAccounts() {
        return serviceAccounts;
    }

    public Set<String> getOrganizations() {
        return organizations;
    }

    @Override
    public boolean implies(Subject subject) {
        return false;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JwtPrincipal that = (JwtPrincipal) o;
        return this.hashcode.equals(that.hashcode);
    }

    @Override
    public int hashCode() {
        return hashcode;
    }
}
