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
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * cloud event test
 *
 * @author leijuan
 */
public class CloudEventTest {

    @Test
    public void testJson() throws Exception {
        // given
        final String eventId = UUID.randomUUID().toString();
        final URI src = URI.create("/trigger");
        final String eventType = "My.Cloud.Event.Type";

        Map<String, Object> data = new HashMap<>();
        data.put("id", 1);
        data.put("welcome", "欢迎");
        // passing in the given attributes
        final CloudEventImpl<Map<String, Object>> cloudEvent = RSocketCloudEventBuilder.<Map<String, Object>>builder()
                .withType(eventType)
                .withId(eventId)
                .withTime(ZonedDateTime.now())
                .withDataschema(URI.create("demo:demo"))
                .withDataContentType("application/json")
                .withSource(src)
                .withData(data)
                .build();
        String text = new String(Json.serialize(cloudEvent), StandardCharsets.UTF_8);
        System.out.println(text);
        text = text.replace("欢迎", "leijuan");
        CloudEvent cloudEvent2 = Json.deserialize(text);
        System.out.println(new String(cloudEvent2.getData().toBytes()));
    }

    @Test
    public void testJsonString() {
        final CloudEventImpl<String> cloudEvent = RSocketCloudEventBuilder.<String>builder()
                .withType("java.lang.String")
                .withId("1111")
                .withTime(ZonedDateTime.now())
                .withDataschema(URI.create("demo:demo"))
                .withDataContentType("application/json")
                .withSource(URI.create("rsocket:source"))
                .withData("Hello")
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
        upstreamClusterChangedEvent.setInterfaceName("com.alibaba.account.AccountService");
        upstreamClusterChangedEvent.setVersion("1.0.0");
        upstreamClusterChangedEvent.setUris(Arrays.asList("demo1", "demo2"));
        // passing in the given attributes
        final CloudEventImpl<UpstreamClusterChangedEvent> cloudEvent = RSocketCloudEventBuilder.<UpstreamClusterChangedEvent>builder()
                .withType("com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent")
                .withId("xxxxx")
                .withTime(ZonedDateTime.now())
                .withDataschema(URI.create("demo:demo"))
                .withDataContentType("application/json")
                .withSource(new URI("demo"))
                .withData(upstreamClusterChangedEvent)
                .build();
        String text = new String(Json.serialize(cloudEvent));
        CloudEventImpl<UpstreamClusterChangedEvent> event2 = Json.decodeValue(text, UpstreamClusterChangedEvent.class);
        UpstreamClusterChangedEvent upstreamClusterChangedEvent1 = CloudEventSupport.unwrapData(event2, UpstreamClusterChangedEvent.class);
        Assertions.assertEquals(upstreamClusterChangedEvent.getInterfaceName(), upstreamClusterChangedEvent1.getInterfaceName());
    }

}
