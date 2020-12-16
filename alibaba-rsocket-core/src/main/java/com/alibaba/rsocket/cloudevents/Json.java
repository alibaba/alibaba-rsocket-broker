package com.alibaba.rsocket.cloudevents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.jackson.PojoCloudEventDataMapper;

import java.nio.charset.StandardCharsets;

/**
 * Json for cloudevents
 *
 * @author leijuan
 */
public class Json {

    private static EventFormat EVENT_FORMAT = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
    public static ObjectMapper MAPPER = new ObjectMapper();

    public static CloudEvent deserialize(String jsonText) {
        return EVENT_FORMAT.deserialize(jsonText.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] serialize(CloudEventImpl<?> event) {
        return EVENT_FORMAT.serialize(event.getCloudEvent());
    }

    public static String serializeAsText(CloudEventImpl<?> event) {
        return new String(EVENT_FORMAT.serialize(event.getCloudEvent()), StandardCharsets.UTF_8);
    }

    public static CloudEventImpl<JsonNode> decodeValue(final String str) throws IllegalStateException {
        CloudEvent cloudEvent = EVENT_FORMAT.deserialize(str.getBytes(StandardCharsets.UTF_8));
        if (cloudEvent.getData() instanceof JsonCloudEventData) {
            JsonCloudEventData jsonCloudEventData = (JsonCloudEventData) cloudEvent.getData();
            return new CloudEventImpl<>(jsonCloudEventData.getNode(), cloudEvent);
        }
        return new CloudEventImpl<>(null, cloudEvent);
    }

    public static <T> CloudEventImpl<T> decodeValue(final String str, Class<T> clazz) throws IllegalStateException {
        CloudEvent cloudEvent = EVENT_FORMAT.deserialize(str.getBytes(StandardCharsets.UTF_8), PojoCloudEventDataMapper.from(MAPPER, clazz));
        if (cloudEvent.getData() instanceof PojoCloudEventData) {
            @SuppressWarnings("unchecked")
            PojoCloudEventData<T> pojoCloudEventData = (PojoCloudEventData<T>) cloudEvent.getData();
            return new CloudEventImpl<>(pojoCloudEventData.getValue(), cloudEvent);
        }
        return new CloudEventImpl<>(null, cloudEvent);
    }
}
