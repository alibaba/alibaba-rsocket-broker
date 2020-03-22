package com.alibaba.rsocket.registry.client.demo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

/**
 * registry client test
 *
 * @author leijuan
 */
@Disabled
public class RegistryClientTest extends RegistryBaseTestCase {
    @Autowired
    private DiscoveryClient discoveryClient;

    @Test
    public void testClient() {
        List<ServiceInstance> instances = discoveryClient.getInstances("rsocket-user-service");
        for (ServiceInstance instance : instances) {
            System.out.println(instance.getHost());
        }
        Assertions.assertTrue(!instances.isEmpty());
    }
}
