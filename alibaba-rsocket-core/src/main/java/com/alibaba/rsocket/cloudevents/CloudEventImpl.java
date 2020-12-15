package com.alibaba.rsocket.cloudevents;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventAttributes;

import java.util.Optional;

/**
 * CloudEvent wrapper with POJO support
 *
 * @author leijuan
 */
public class CloudEventImpl<T> {
    private final T data;
    private CloudEvent cloudEvent;

    public CloudEventImpl(T data, CloudEvent cloudEvent) {
        this.data = data;
        this.cloudEvent = cloudEvent;
    }

    public Optional<T> getData() {
        return Optional.ofNullable(data);
    }


    public CloudEventAttributes getAttributes() {
        return cloudEvent;
    }

    public CloudEvent getCloudEvent() {
        return cloudEvent;
    }
}
