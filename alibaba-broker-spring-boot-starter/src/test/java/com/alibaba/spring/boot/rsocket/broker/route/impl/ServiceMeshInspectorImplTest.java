package com.alibaba.spring.boot.rsocket.broker.route.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * service mesh inspector implementation test
 *
 * @author leijuan
 */
public class ServiceMeshInspectorImplTest {
    private ServiceMeshInspectorImpl serviceMeshInspector = new ServiceMeshInspectorImpl();

    @Test
    public void testAuth() throws Exception {
        String routing="com.alibaba.user.UserService";
        Set<String> serviceAccounts = toSet("serviceAccount1", "serviceAccount2");
        Set<String> serviceAccounts2 = toSet("serviceAccount3", "serviceAccount4");
        Set<String> orgs = toSet("org1", "or2");
        Set<String> orgs2 = toSet("org3", "or4");
        RSocketAppPrincipalMock requesterPrincipal = new RSocketAppPrincipalMock("app1", orgs, serviceAccounts);
        RSocketAppPrincipalMock responderPrincipal = new RSocketAppPrincipalMock("app1", orgs, serviceAccounts);
        Assertions.assertTrue(serviceMeshInspector.isRequestAllowed(requesterPrincipal, routing, responderPrincipal));
        Assertions.assertFalse(serviceMeshInspector.isRequestAllowed(requesterPrincipal, routing,
                new RSocketAppPrincipalMock("app1", orgs2, serviceAccounts)));
        Assertions.assertFalse(serviceMeshInspector.isRequestAllowed(requesterPrincipal, routing,
                new RSocketAppPrincipalMock("app1", orgs, serviceAccounts2)));
    }

    private Set<String> toSet(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }
}
