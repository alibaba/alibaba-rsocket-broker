package com.alibaba.rsocket.metadata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * GSV Routing Metadata test
 *
 * @author leijuan
 */
public class GSVRoutingMetadataTest {

    @Test
    public void testParse() {
        GSVRoutingMetadata routing = new GSVRoutingMetadata();
        routing.setGroup("dc1");
        routing.setService("com.alibaba.UserService");
        routing.setMethod("findById");
        routing.setVersion("1.0.0");
        //routing.setEndpoint("192.168.0.1");
        String routingStr = routing.formatData();
        System.out.println(routingStr);
        GSVRoutingMetadata routing2 = GSVRoutingMetadata.from(routing.getContent());
        String routingStr2 = routing2.formatData();
        System.out.println(routingStr2);
        Assertions.assertEquals(routingStr, routingStr2);
    }

    @Test
    public void testEmptyGroupAndVersion() {
        GSVRoutingMetadata routing = new GSVRoutingMetadata();
        routing.setService("com.alibaba.UserService");
        routing.setMethod("findById");
        String routingStr = routing.formatData();
        System.out.println(routingStr);
        GSVRoutingMetadata routing2 = GSVRoutingMetadata.from(routing.getContent());
        String routingStr2 = routing2.formatData();
        System.out.println(routingStr2);
        Assertions.assertEquals(routingStr, routingStr2);
    }
}
