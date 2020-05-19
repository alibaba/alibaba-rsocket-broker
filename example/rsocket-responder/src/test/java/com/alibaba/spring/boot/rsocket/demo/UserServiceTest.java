package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.uri.UriTransportRegistry;
import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.*;

/**
 * user service test
 *
 * @author leijuan
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class UserServiceTest {
    private ObjectMapper objectMapper = new ObjectMapper();
    RSocket rsocket;

    @BeforeAll
    public void setUp() {
        rsocket = RSocketConnector.create()
                .connect(UriTransportRegistry.clientForUri("tcp://127.0.0.1:42252"))
                .block();
    }

    @AfterAll
    public void tearDown() throws Exception {
        rsocket.dispose();
    }

    @Test
    public void testFindById() throws Exception {
        RSocketCompositeMetadata compositeMetadata = new RSocketCompositeMetadata();
        GSVRoutingMetadata routingMetadata = new GSVRoutingMetadata("", "com.alibaba.user.UserService2", "findById", "");
        compositeMetadata.addMetadata(routingMetadata);
        MessageMimeTypeMetadata dataEncodingMetadata = new MessageMimeTypeMetadata(WellKnownMimeType.APPLICATION_JSON);
        compositeMetadata.addMetadata(dataEncodingMetadata);
        rsocket.requestResponse(DefaultPayload.create(Unpooled.wrappedBuffer(objectMapper.writeValueAsBytes(1)), compositeMetadata.getContent()))
                .doOnTerminate(() -> {
                    ReferenceCountUtil.safeRelease(compositeMetadata);
                })
                .subscribe(payload -> {
                    System.out.println(payload.getDataUtf8());
                });
        Thread.sleep(1000);
    }
}
