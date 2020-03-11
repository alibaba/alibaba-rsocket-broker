package com.alibaba.spring.boot.rsocket.broker.cluster.scalecube.codec.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.cloudevents.json.ZonedDateTimeDeserializer;
import io.cloudevents.json.ZonedDateTimeSerializer;

import java.time.ZonedDateTime;

/**
 * CloudEvents Jackson module
 *
 * @author leijuan
 */
public class CloudEventsModule extends SimpleModule {

    public CloudEventsModule() {
        this.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        this.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
    }
}
