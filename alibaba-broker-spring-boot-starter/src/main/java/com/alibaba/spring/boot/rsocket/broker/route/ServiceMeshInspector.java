package com.alibaba.spring.boot.rsocket.broker.route;

import com.alibaba.spring.boot.rsocket.broker.security.RSocketAppPrincipal;

/**
 * Service Mesh inspector
 *
 * @author leijuan
 */
public interface ServiceMeshInspector {

    boolean isRequestAllowed(RSocketAppPrincipal requesterPrincipal, String routing, RSocketAppPrincipal responderPrincipal);

    Integer getWhiteRelationCount();
}
