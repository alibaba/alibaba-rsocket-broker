package com.alibaba.rsocket.metadata;

import io.cloudevents.json.Json;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.UUID;

/**
 * composite metadata test
 *
 * @author leijuan
 */
public class RSocketCompositeMetadataTest {

    @Test
    public void testCompositeMetadata() throws Exception {
        RSocketCompositeMetadata compositeMetadata = new RSocketCompositeMetadata();
        AppMetadata appMetadata = new AppMetadata();
        appMetadata.setUuid(UUID.randomUUID().toString());
        appMetadata.setName("user-service");
        appMetadata.setIp("192.168.0.1");
        appMetadata.setPort(42252);
        HashMap<String, String> tempMap = new HashMap<>();
        tempMap.put("first", "second");
        appMetadata.setMetadata(tempMap);
        compositeMetadata.addMetadata(appMetadata);
        compositeMetadata.addMetadata(new MessageMimeTypeMetadata(RSocketMimeType.Hessian));
        compositeMetadata.addMetadata(new BearerTokenMetadata("xxx.yyy.zz".toCharArray()));
        compositeMetadata.addMetadata(new GSVRoutingMetadata("", "com.alibaba.UserService", "findById", "1.0.0"));
        ByteBuf buffer = compositeMetadata.getContent();
        //System.out.println(buffer.capacity());
        RSocketCompositeMetadata temp = RSocketCompositeMetadata.from(Unpooled.wrappedBuffer(buffer.array()));
        Assertions.assertNotNull(temp.getRoutingMetaData());
        String text = temp.toText();
        System.out.println(text);
        RSocketCompositeMetadata temp2 = new RSocketCompositeMetadata();
        temp2.load(text);
        AppMetadata appMetadata1 = AppMetadata.from(temp2.getMetadata(RSocketMimeType.Application));
        System.out.println(appMetadata1.getIp());
        Assertions.assertNotNull(temp2.getRoutingMetaData());
        compositeMetadata.getMetadata(RSocketMimeType.Application);
    }

    @Test
    public void testSetupMetadata() throws Exception {
        RSocketCompositeMetadata compositeMetadata = new RSocketCompositeMetadata();
        ByteBuf buffer = compositeMetadata.getContent();
        //System.out.println(buffer.capacity());
        RSocketCompositeMetadata temp = RSocketCompositeMetadata.from(Unpooled.wrappedBuffer(buffer.array()));
        System.out.println(temp.getMetadata(RSocketMimeType.ServiceRegistry));
    }

    @Test
    public void testMimetype() {
        System.out.println(RSocketMimeType.valueOf((byte) 5));
    }

    @Test
    public void testCloudEvents() throws Exception {
        final CloudEventImpl<String> cloudEvent = CloudEventBuilder.<String>builder()
                .withType("eventType")
                .withId("xxxx")
                .withTime(ZonedDateTime.now())
                .withDataschema(URI.create("demo:demo"))
                .withDataContentType("text/plain")
                .withSource(URI.create("app://app1"))
                .withData("欢迎")
                .build();
        Payload payload = cloudEventToPayload(cloudEvent);
        payload.getMetadata().rewind();
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
        MessageMimeTypeMetadata dataEncodingMetadata = compositeMetadata.getDataEncodingMetadata();
        Assertions.assertNotNull(dataEncodingMetadata);
        System.out.println(dataEncodingMetadata.getMimeType());
    }

    public static Payload cloudEventToPayload(CloudEventImpl<?> cloudEvent) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(new MessageMimeTypeMetadata(RSocketMimeType.CloudEventsJson));
        return ByteBufPayload.create(Unpooled.wrappedBuffer(Json.binaryEncode(cloudEvent)), compositeMetadata.getContent());
    }
}
