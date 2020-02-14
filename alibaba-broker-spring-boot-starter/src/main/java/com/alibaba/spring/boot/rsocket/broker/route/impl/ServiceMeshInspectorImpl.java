package com.alibaba.spring.boot.rsocket.broker.route.impl;

import com.alibaba.spring.boot.rsocket.broker.route.ServiceMeshInspector;
import com.alibaba.spring.boot.rsocket.broker.security.RSocketAppPrincipal;
import org.roaringbitmap.RoaringBitmap;

/**
 * Service mesh inspector implementation
 *
 * @author leijuan
 */
public class ServiceMeshInspectorImpl implements ServiceMeshInspector {
    /**
     * white relation bitmap
     */
    private RoaringBitmap whiteRelationBitmap = new RoaringBitmap();
    /**
     * auth required
     */
    private boolean authRequired = true;

    public ServiceMeshInspectorImpl() {

    }

    public ServiceMeshInspectorImpl(boolean authRequired) {
        this.authRequired = authRequired;
    }

    public void setAuthRequired(boolean authRequired) {
        this.authRequired = authRequired;
    }

    @Override
    public boolean isRequestAllowed(RSocketAppPrincipal requesterPrincipal, String routing, RSocketAppPrincipal responderPrincipal) {
        if (!authRequired) return true;
        //org & service account relation
        int relationHashCode = (requesterPrincipal.hashCode() + ":" + responderPrincipal.hashCode()).hashCode();
        if (whiteRelationBitmap.contains(relationHashCode)) {
            return true;
        }
        //acl mapping
        int aclHashCode = (requesterPrincipal.hashCode() + ":" + routing + ":" + responderPrincipal.hashCode()).hashCode();
        if (whiteRelationBitmap.contains(aclHashCode)) {
            return true;
        }
        boolean orgFriendly = false;
        for (String principalOrg : requesterPrincipal.getOrganizations()) {
            if (responderPrincipal.getOrganizations().contains(principalOrg)) {
                orgFriendly = true;
                break;
            }
        }
        if (orgFriendly) {
            boolean serviceAccountFriendly = false;
            for (String serviceAccount : requesterPrincipal.getServiceAccounts()) {
                if (responderPrincipal.getServiceAccounts().contains(serviceAccount)) {
                    serviceAccountFriendly = true;
                    break;
                }
            }
            if (serviceAccountFriendly) {
                whiteRelationBitmap.add(relationHashCode);
                return true;
            }
        }
        return false;
    }

    @Override
    public Integer getWhiteRelationCount() {
        return whiteRelationBitmap.getCardinality();
    }
}
