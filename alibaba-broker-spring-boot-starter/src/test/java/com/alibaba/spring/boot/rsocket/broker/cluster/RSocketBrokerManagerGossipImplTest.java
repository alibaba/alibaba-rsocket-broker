package com.alibaba.spring.boot.rsocket.broker.cluster;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;
import com.alibaba.rsocket.events.AppStatusEvent;
import io.scalecube.cluster.codec.jackson.JacksonMessageCodec;
import io.scalecube.cluster.transport.api.Message;
import io.scalecube.cluster.transport.api.MessageCodec;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
        CloudEventImpl<AppStatusEvent> cloudEvent = RSocketCloudEventBuilder.builder(appStatusEvent)
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
