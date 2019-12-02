package com.alibaba.rsocket.event;

import com.alibaba.rsocket.encoding.JsonUtils;
import com.alibaba.rsocket.events.CloudEventSupport;
import com.alibaba.rsocket.events.ConfigEvent;
import com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cloudevents.json.Json;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Arrays;
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

        // passing in the given attributes
        final CloudEventImpl<String> cloudEvent = CloudEventBuilder.<String>builder()
                .withType(eventType)
                .withId(eventId)
                .withTime(ZonedDateTime.now())
                .withDataschema(URI.create("demo:demo"))
                .withDataContentType("text/plain")
                .withSource(src)
                .withData("欢迎")
                .build();
        String text = Json.encode(cloudEvent);
        System.out.println(text);
        text = text.replace("欢迎", "leijuan");
        Json.decodeValue(text, new TypeReference<CloudEventImpl<String>>() {
        });
        System.out.println(cloudEvent.getData().get());
    }

    @Test
    public void testCloudEvent() throws Exception {
        UpstreamClusterChangedEvent upstreamClusterChangedEvent = new UpstreamClusterChangedEvent();
        upstreamClusterChangedEvent.setGroup("demo");
        upstreamClusterChangedEvent.setInterfaceName("com.alibaba.account.AccountService");
        upstreamClusterChangedEvent.setVersion("1.0.0");
        upstreamClusterChangedEvent.setUris(Arrays.asList("demo1", "demo2"));
        // passing in the given attributes
        final CloudEventImpl<UpstreamClusterChangedEvent> cloudEvent = CloudEventBuilder.<UpstreamClusterChangedEvent>builder()
                .withType("com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent")
                .withId("xxxxx")
                .withTime(ZonedDateTime.now())
                .withDataschema(URI.create("demo:demo"))
                .withDataContentType("application/json")
                .withSource(new URI("demo"))
                .withData(upstreamClusterChangedEvent)
                .build();
        String text = Json.encode(cloudEvent);
        CloudEventImpl<UpstreamClusterChangedEvent> event2 = Json.decodeValue(text, new TypeReference<CloudEventImpl<UpstreamClusterChangedEvent>>() {
        });
        UpstreamClusterChangedEvent upstreamClusterChangedEvent1 = CloudEventSupport.unwrapData(event2, UpstreamClusterChangedEvent.class);
        System.out.println(Json.encode(upstreamClusterChangedEvent1));
        Assertions.assertEquals(upstreamClusterChangedEvent.getInterfaceName(), upstreamClusterChangedEvent1.getInterfaceName());
    }

    @Test
    public void testConvert() throws Exception {
        ConfigEvent configurationEvent = new ConfigEvent("app1", "text/plain", "Hello");
        CloudEventImpl<ConfigEvent> cloudEvent = configurationEvent.toCloudEvent(URI.create("demo"));
        String jsonText = Json.encode(cloudEvent);
        System.out.println(jsonText);
        CloudEventImpl<ConfigEvent> configurationEvent2 = Json.decodeValue(jsonText, new TypeReference<CloudEventImpl<ConfigEvent>>() {
        });
        System.out.println(Json.encode(configurationEvent2));
    }
}
