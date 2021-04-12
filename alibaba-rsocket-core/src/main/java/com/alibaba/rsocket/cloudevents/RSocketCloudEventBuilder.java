package com.alibaba.rsocket.cloudevents;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.transport.NetworkUtil;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import io.rsocket.metadata.WellKnownMimeType;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * CloudEvent builder for RSocket broker
 *
 * @author leijuan
 */
public class RSocketCloudEventBuilder<T> {
    private CloudEventBuilder builder = CloudEventBuilder.v1().withDataContentType(WellKnownMimeType.APPLICATION_JSON.getString());
    private T data;
    private static URI DEFAULT_SOURCE = URI.create("app://" + RSocketAppContext.ID + "/" + "?ip=" + NetworkUtil.LOCAL_IP);

    /**
     * Gets a brand new builder instance
     *
     * @param <T> The 'data' type
     */
    public static <T> RSocketCloudEventBuilder<T> builder() {
        return new RSocketCloudEventBuilder<>();
    }

    /**
     * builder with UUID, application/json, now timestamp, Class full name as type and default sources
     *
     * @param data data
     * @param <T>  data type
     * @return cloud event builder
     */
    public static <T> RSocketCloudEventBuilder<T> builder(T data) {
        RSocketCloudEventBuilder<T> builder = new RSocketCloudEventBuilder<>();
        builder.data = data;
        builder
                .withId(UUID.randomUUID().toString())
                .withDataContentType(WellKnownMimeType.APPLICATION_JSON.getString())
                .withTime(OffsetDateTime.now())
                .withType(data.getClass().getCanonicalName())
                .withSource(DEFAULT_SOURCE);
        return builder;
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

    public RSocketCloudEventBuilder<T> withTime(OffsetDateTime time) {
        this.builder.withTime(time);
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
