package com.alibaba.spring.boot.rsocket.broker.security;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.Principal;

/**
 * AuthenticationServiceJwtImpl test
 *
 * @author leijuan
 */
public class AuthenticationServiceJwtImplTest {
    public AuthenticationServiceJwtImpl authenticationService;

    @BeforeAll
    public void setUp() throws Exception {
        this.authenticationService = new AuthenticationServiceJwtImpl();
    }

    @Test
    public void testAuth() throws Exception {
        String subject = "testing-only";
        String credentials = authenticationService.generateCredentials(new String[]{"alibaba"}, new String[]{"default"}, new String[]{"internal"}, null, subject, new String[]{"leijuan"});
        System.out.println(credentials);
        Principal principal = authenticationService.auth("JWT", credentials);
        Assertions.assertNotNull(principal);
        Assertions.assertEquals(subject, principal.getName());
    }

}
