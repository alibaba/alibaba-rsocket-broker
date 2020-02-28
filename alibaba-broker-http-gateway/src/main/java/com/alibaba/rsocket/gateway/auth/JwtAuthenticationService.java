package com.alibaba.rsocket.gateway.auth;

import org.jetbrains.annotations.Nullable;

/**
 * JWT auth service
 *
 * @author leijuan
 */
public interface JwtAuthenticationService {
    @Nullable
    NamedPrincipal auth(String jwtToken);
}
