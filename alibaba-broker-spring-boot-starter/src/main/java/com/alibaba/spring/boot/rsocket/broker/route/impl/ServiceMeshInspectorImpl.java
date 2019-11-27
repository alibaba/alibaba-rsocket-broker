package com.alibaba.spring.boot.rsocket.broker.route.impl;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.utils.MurmurHash3;
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

    @Override
    public boolean isRequestAllowed(RSocketAppPrincipal requesterPrincipal, String routing, RSocketAppPrincipal responderPrincipal) {
        //org & service account relation
        int relationHashCode = MurmurHash3.hash32(requesterPrincipal.hashCode() + ":" + responderPrincipal.hashCode());
        if (whiteRelationBitmap.contains(relationHashCode)) {
            return true;
        }
        //acl mapping
        int aclHashCode = MurmurHash3.hash32(requesterPrincipal.hashCode() + ":" + routing + ":" + responderPrincipal.hashCode());
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
