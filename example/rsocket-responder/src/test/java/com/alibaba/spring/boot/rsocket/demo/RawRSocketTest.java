package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.encoding.JsonUtils;
import com.alibaba.rsocket.metadata.ServiceRegistryMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.metadata.CompositeMetadataCodec;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

/**
 * RawRSocket test
 *
 * @author leijuan
 */
public class RawRSocketTest {

    @Test
    public void testFindUserById() throws Exception {
        RSocket rsocket = createBrokerClient();
        Payload result = rsocket.requestResponse(requestPayload("com.alibaba.user.UserService.findById", "[1]".getBytes())).block();
        System.out.println(result.getDataUtf8());
        rsocket.dispose();
    }

    @Test
    public void testAppMetadataLoad() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(1));
    }


    @Test
    public void testService() throws Exception {
        ServiceRegistryMetadata serviceRegistryMetadata = new ServiceRegistryMetadata();
        serviceRegistryMetadata.addPublishedService(new ServiceLocator(null, "com.alibaba.user.UserService", null));
        System.out.println(JsonUtils.toJsonText(serviceRegistryMetadata));
    }

    public static RSocket createBrokerClient() {
        return RSocketConnector.create()
                .setupPayload(setupPayload())
                .dataMimeType(WellKnownMimeType.APPLICATION_JSON.getString())
                .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString())
                .connect(TcpClientTransport.create("127.0.0.1", 9999))
                .block();
    }


    public static Payload setupPayload() {
        CompositeByteBuf buf = new CompositeByteBuf(ByteBufAllocator.DEFAULT, false, 8);
        // language=json
        String appInfo = "{\n" +
                "    \"uuid\": \"" + UUID.randomUUID().toString() + "\",\n" +
                "    \"name\" : \"app-1\",\n" +
                "    \"ip\": \"192.168.1.2\"\n" +
                "}";
        CompositeMetadataCodec.encodeAndAddMetadata(buf, ByteBufAllocator.DEFAULT,
                "message/x.rsocket.application+json",
                Unpooled.wrappedBuffer(appInfo.getBytes()));
       /* // language=json
        String exposedServices = "{\"published\":[{\"group\":null,\"service\":\"com.alibaba.YourService\",\"version\":null}]}\n";
        CompositeMetadataCodec.encodeAndAddMetadata(buf, ByteBufAllocator.DEFAULT,
                "message/x.rsocket.service.registry.v0+json",
                Unpooled.wrappedBuffer(exposedServices.getBytes()));*/
        return DefaultPayload.create(Unpooled.EMPTY_BUFFER, buf);
    }

    public static Payload requestPayload(String routing, byte[] data) {
        CompositeByteBuf buf = new CompositeByteBuf(ByteBufAllocator.DEFAULT, false, 8);
        ByteBuf routingContent = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList(routing));
        // routing metadata
        CompositeMetadataCodec.encodeAndAddMetadata(buf, ByteBufAllocator.DEFAULT, WellKnownMimeType.MESSAGE_RSOCKET_ROUTING, routingContent);
        // encoding metadata
        CompositeMetadataCodec.encodeAndAddMetadata(buf, ByteBufAllocator.DEFAULT, WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE, constructJsonDataEncoding());
        return DefaultPayload.create(Unpooled.wrappedBuffer(data), buf);
    }

    private static ByteBuf constructJsonDataEncoding() {
        return Unpooled.wrappedBuffer(new byte[]{(byte) (WellKnownMimeType.APPLICATION_JSON.getIdentifier() | 0x80)});
    }
}
