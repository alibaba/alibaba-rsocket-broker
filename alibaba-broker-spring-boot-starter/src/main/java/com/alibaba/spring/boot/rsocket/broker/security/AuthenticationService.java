package com.alibaba.spring.boot.rsocket.broker.security;

import org.jetbrains.annotations.Nullable;

/**
 * authentication service
 *
 * @author leijuan
 */
public interface AuthenticationService {

    @Nullable
    RSocketAppPrincipal auth(String type, String credentials);

    String generateCredentials(String id, String[] organizations, String[] serviceAccounts, String[] roles, String[] authorities, String sub, String[] audience) throws Exception;

}
