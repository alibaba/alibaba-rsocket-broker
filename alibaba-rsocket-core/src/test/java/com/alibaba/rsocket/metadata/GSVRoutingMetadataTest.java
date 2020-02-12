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
        String gsv = routingMetadata.gsv();
        System.out.println(gsv);
        assertThat(gsv).contains(":");

    }

    @Test
    public void testEmptyGroupAndVersion() {
        GSVRoutingMetadata routing = new GSVRoutingMetadata();
        routing.setService("com.alibaba.UserService");
        routing.setMethod("findById");
        String gsv = routing.gsv();
        System.out.println(gsv);
        assertThat(gsv).doesNotContain(":");
    }
}
