package com.alibaba.rsocket.metadata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Arrays;
import java.util.List;

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

    @Test
    public void testSticky() {
        List<Tuple2<Boolean, String>> tuples = Arrays.asList(Tuples.of(true, ""), Tuples.of(false, "id:xxxx"));
        for (Tuple2<Boolean, String> tuple : tuples) {
            GSVRoutingMetadata routing = new GSVRoutingMetadata();
            routing.setService("com.alibaba.UserService");
            routing.setMethod("findById");
            routing.setSticky(tuple.getT1());
            routing.setEndpoint(tuple.getT2());
            String routingKey = routing.assembleRoutingKey();
            GSVRoutingMetadata routing2 = GSVRoutingMetadata.from(routingKey);
            Assertions.assertEquals(routing.isSticky(), routing2.isSticky());
            Assertions.assertEquals(routing.getService(), routing2.getService());
            Assertions.assertEquals(routing.getMethod(), routing2.getMethod());
            Assertions.assertEquals(routing.getEndpoint(), routing2.getEndpoint());
        }

    }
}
