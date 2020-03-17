package com.alibaba.rsocket.encoding.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.cloudevents.json.ZonedDateTimeDeserializer;
import io.cloudevents.json.ZonedDateTimeSerializer;

import java.time.ZonedDateTime;

/**
 * CloudEvents Jackson databind Module
 *
 * @author leijuan
 */
public class CloudEventsModule extends SimpleModule {
    public CloudEventsModule() {
        this.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        this.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
    }
}
