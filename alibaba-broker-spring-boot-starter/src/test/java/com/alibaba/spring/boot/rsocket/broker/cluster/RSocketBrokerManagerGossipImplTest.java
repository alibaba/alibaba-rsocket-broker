package com.alibaba.spring.boot.rsocket.broker.cluster;

import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.spring.boot.rsocket.broker.cluster.scalecube.codec.jackson.JacksonMessageCodec;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import io.scalecube.cluster.transport.api.Message;
import io.scalecube.cluster.transport.api.MessageCodec;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * RSocket broker manager gossip implement test
 *
 * @author leijuan
 */
public class RSocketBrokerManagerGossipImplTest {
    private MessageCodec messageCodec = MessageCodec.INSTANCE;

    @Test
    public void testDefaultScalecubeMessageCodec() {
        MessageCodec messageCodec = MessageCodec.INSTANCE;
        Assertions.assertThat(messageCodec.getClass()).isEqualTo(JacksonMessageCodec.class);
    }

    @Test
    public void testGossipMessageEncoding() throws Exception {
        Map<String, String> value = new HashMap<>();
        value.put("nick", "leijuan");
        Message message = Message.builder().correlationId("1").data(value).build();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        messageCodec.serialize(message, bos);
        Message message1 = messageCodec.deserialize(new ByteArrayInputStream(bos.toByteArray()));
        Map<String, String> value2 = message1.data();
        Assertions.assertThat(value).containsEntry("nick", value2.get("nick"));
    }

    @Test
    public void testGossipMessageWithCloudEvents() throws Exception {
        AppStatusEvent appStatusEvent = new AppStatusEvent("1", 1);
        CloudEventImpl<AppStatusEvent> cloudEvent = CloudEventBuilder.<AppStatusEvent>builder()
                .withId("1")
                .withSource(URI.create("app://1"))
                .withType("type1")
                .withTime(ZonedDateTime.now())
                .withDataContentType("application/json")
                .withData(appStatusEvent)
                .withSubject("app status update")
                .build();
        Message message = Message.builder().correlationId("1").data(cloudEvent).build();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        messageCodec.serialize(message, bos);
        Message message1 = messageCodec.deserialize(new ByteArrayInputStream(bos.toByteArray()));
        CloudEventImpl<AppStatusEvent> cloudEvent2 = message1.data();
        Assertions.assertThat(cloudEvent2.getData()).isPresent();
    }
}
