package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.DataEncodingMetadata;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import io.netty.buffer.Unpooled;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.uri.UriTransportRegistry;
import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * user service test
 *
 * @author leijuan
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserServiceTest {
    RSocket rsocket;

    @BeforeAll
    public void setUp() throws Exception {
        rsocket = RSocketFactory.connect()
                .transport(UriTransportRegistry.clientForUri("tcp://127.0.0.1:42252"))
                .start()
                .block();
    }

    @AfterAll
    public void tearDown() throws Exception {
        rsocket.dispose();
    }

    @Test
    public void testFindById() throws Exception {
        String data = "[1]";
        RSocketCompositeMetadata compositeMetadata = new RSocketCompositeMetadata();
        GSVRoutingMetadata routingMetadata = new GSVRoutingMetadata();
        routingMetadata.load("g:,s:com.alibaba.UserService,m:findById,v:1.0.0,e:");
        compositeMetadata.addMetadata(routingMetadata);
        DataEncodingMetadata dataEncodingMetadata = new DataEncodingMetadata();
        dataEncodingMetadata.load("5:5");
        compositeMetadata.addMetadata(dataEncodingMetadata);
        rsocket.requestResponse(DefaultPayload.create(Unpooled.wrappedBuffer(data.getBytes()), compositeMetadata.getContent()))
                .subscribe(payload -> {
                    System.out.println(payload.getDataUtf8());
                });
    }
}
