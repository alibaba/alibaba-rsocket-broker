package com.alibaba.rsocket.cloudevents;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import io.rsocket.metadata.WellKnownMimeType;

import java.net.URI;
import java.time.ZonedDateTime;

/**
 * CloudEvent builder for RSocket broker
 *
 * @author leijuan
 */
public class RSocketCloudEventBuilder<T> {
    private CloudEventBuilder builder = CloudEventBuilder.v1().withDataContentType(WellKnownMimeType.APPLICATION_JSON.getString());
    private T data;

    /**
     * Gets a brand new builder instance
     *
     * @param <T> The 'data' type
     */
    public static <T> RSocketCloudEventBuilder<T> builder() {
        return new RSocketCloudEventBuilder<>();
    }


    public RSocketCloudEventBuilder<T> withId(String id) {
        this.builder.withId(id);
        return this;
    }

    public RSocketCloudEventBuilder<T> withSource(URI source) {
        this.builder.withSource(source);
        return this;
    }

    public RSocketCloudEventBuilder<T> withType(String type) {
        this.builder.withType(type);
        return this;
    }

    public RSocketCloudEventBuilder<T> withDataschema(URI dataschema) {
        this.builder.withDataSchema(dataschema);
        return this;
    }

    public RSocketCloudEventBuilder<T> withDataContentType(String datacontenttype) {
        this.builder.withDataContentType(datacontenttype);
        return this;
    }

    public RSocketCloudEventBuilder<T> withSubject(String subject) {
        this.builder.withSubject(subject);
        return this;
    }

    public RSocketCloudEventBuilder<T> withTime(ZonedDateTime time) {
        this.builder.withTime(time.toOffsetDateTime());
        return this;
    }

    public RSocketCloudEventBuilder<T> withData(T data) {
        this.data = data;
        return this;
    }

    public CloudEventImpl<T> build() {
        CloudEvent cloudEvent = builder.withData(PojoCloudEventData.wrap(this.data, data1 -> Json.MAPPER.writeValueAsBytes(data1))).build();
        return new CloudEventImpl<>(data, cloudEvent);
    }
}
