package com.alibaba.rsocket.events;

import com.alibaba.rsocket.encoding.JsonUtils;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * cloud event support
 *
 * @author leijuan
 */
public interface CloudEventSupport<T extends CloudEventSupport<?>> {

    @Nullable
    static <T> T unwrapData(CloudEventImpl<?> cloudEvent, Class<T> targetClass) {
        return cloudEvent.getData().map(data -> {
            try {
                if (data instanceof ObjectNode || data instanceof Map) {
                    return JsonUtils.convertValue(data, targetClass);
                } else if (data.getClass().isAssignableFrom(targetClass)) {
                    return (T) data;
                } else if (data instanceof String) {
                    return JsonUtils.readJsonValue((String) data, targetClass);
                }
            } catch (Exception ignore) {
            }
            return null;
        }).orElse(null);
    }

    default CloudEventImpl<T> toCloudEvent(URI source) {
        CloudEventBuilder<T> builder = CloudEventBuilder.builder();
        builder.withId(UUID.randomUUID().toString());
        builder.withType(this.getClass().getCanonicalName());
        builder.withDataContentType(RSocketMimeType.CloudEventsJson.getType());
        builder.withSource(source);
        builder.withTime(ZonedDateTime.now());
        builder.withData((T) this);
        return builder.build();
    }
}
