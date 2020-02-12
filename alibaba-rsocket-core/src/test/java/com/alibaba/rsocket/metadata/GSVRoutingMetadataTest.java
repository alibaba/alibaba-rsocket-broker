package com.alibaba.rsocket.metadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GSV Routing Metadata test
 *
 * @author leijuan
 */
public class GSVRoutingMetadataTest {

    @Test
    public void testParse() {
        GSVRoutingMetadata routingMetadata = new GSVRoutingMetadata();
        routingMetadata.setGroup("dc1");
        routingMetadata.setService("com.alibaba.UserService");
        routingMetadata.setMethod("findById");
        routingMetadata.setVersion("1.0.0");
        //routingMetadata.setEndpoint("192.168.0.1");
        String routing = routingMetadata.routing();
        System.out.println(routing);
        assertThat(routing).contains(":");

    }

    @Test
    public void testEmptyGroupAndVersion() {
        GSVRoutingMetadata routing = new GSVRoutingMetadata();
        routing.setService("com.alibaba.UserService");
        routing.setMethod("findById");
        String routingStr = routing.routing();
        System.out.println(routingStr);
        assertThat(routingStr).doesNotContain(":");
    }
}
