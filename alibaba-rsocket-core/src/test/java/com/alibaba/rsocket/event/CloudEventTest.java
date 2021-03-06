package com.alibaba.rsocket.event;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.Json;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;
import com.alibaba.rsocket.events.CloudEventSupport;
import com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent;
import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * cloud event test
 *
 * @author leijuan
 */
public class CloudEventTest {

    @Test
    public void testJson() throws Exception {
        // given
        final URI src = URI.create("/trigger");
        Map<String, Object> data = new HashMap<>();
        data.put("id", 1);
        data.put("welcome", "欢迎");
        // passing in the given attributes
        final CloudEventImpl<Map<String, Object>> cloudEvent = RSocketCloudEventBuilder.builder(data)
                .withSource(src)
                .build();
        String text = new String(Json.serialize(cloudEvent), StandardCharsets.UTF_8);
        System.out.println(text);
        text = text.replace("欢迎", "leijuan");
        CloudEvent cloudEvent2 = Json.deserialize(text);
        System.out.println(new String(cloudEvent2.getData().toBytes()));
    }

    @Test
    public void testJsonString() {
        final CloudEventImpl<String> cloudEvent = RSocketCloudEventBuilder.builder("Hello")
                .build();
        String text = new String(Json.serialize(cloudEvent), StandardCharsets.UTF_8);
        System.out.println(text);
        text = text.replace("欢迎", "leijuan");
        CloudEventImpl<String> cloudEvent2 = Json.decodeValue(text, String.class);
        System.out.println(cloudEvent2.getData().get());
    }

    @Test
    public void testCloudEvent() throws Exception {
        UpstreamClusterChangedEvent upstreamClusterChangedEvent = new UpstreamClusterChangedEvent();
        upstreamClusterChangedEvent.setGroup("demo");
        upstreamClusterChangedEvent.setInterfaceName("com.alibaba.user.AccountService");
        upstreamClusterChangedEvent.setVersion("1.0.0");
        upstreamClusterChangedEvent.setUris(Arrays.asList("demo1", "demo2"));
        // passing in the given attributes
        final CloudEventImpl<UpstreamClusterChangedEvent> cloudEvent = RSocketCloudEventBuilder
                .builder(upstreamClusterChangedEvent)
                .build();
        String text = new String(Json.serialize(cloudEvent));
        CloudEventImpl<UpstreamClusterChangedEvent> event2 = Json.decodeValue(text, UpstreamClusterChangedEvent.class);
        UpstreamClusterChangedEvent upstreamClusterChangedEvent1 = CloudEventSupport.unwrapData(event2, UpstreamClusterChangedEvent.class);
        Assertions.assertEquals(upstreamClusterChangedEvent.getInterfaceName(), upstreamClusterChangedEvent1.getInterfaceName());
    }

}
