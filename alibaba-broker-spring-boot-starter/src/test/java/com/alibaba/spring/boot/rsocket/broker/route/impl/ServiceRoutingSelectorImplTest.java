package com.alibaba.spring.boot.rsocket.broker.route.impl;

import com.alibaba.rsocket.utils.MurmurHash3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * ServiceRoutingSelectorImpl test
 *
 * @author leijuan
 */
public class ServiceRoutingSelectorImplTest {
    ServiceRoutingSelectorImpl routingSelector = new ServiceRoutingSelectorImpl();

    @Test
    public void testOperation() {
        Integer instanceId = 1;
        Set<String> services = new HashSet<>();
        services.add("1");
        services.add("2");
        services.add("3");
        routingSelector.register(instanceId, services);
        Assertions.assertTrue(routingSelector.containInstance(instanceId));
        Assertions.assertTrue(routingSelector.containService(MurmurHash3.hash32("1")));
        Assertions.assertNotNull(routingSelector.findHandler(MurmurHash3.hash32("1")));
        Assertions.assertNull(routingSelector.findHandler(MurmurHash3.hash32("4")));
        Assertions.assertEquals(1, routingSelector.getInstanceCount(MurmurHash3.hash32("1")));
        routingSelector.deregister(instanceId);
        Assertions.assertNull(routingSelector.findHandler(MurmurHash3.hash32("1")));
    }
}
