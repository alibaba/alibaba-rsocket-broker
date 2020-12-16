package com.alibaba.rsocket.events;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;
import com.alibaba.rsocket.encoding.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Map;

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
        return RSocketCloudEventBuilder.builder((T) this).withSource(source).build();
    }
}
