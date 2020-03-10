package com.alibaba.spring.boot.rsocket.broker.cluster.scaleccube.codec.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.cloudevents.json.ZonedDateTimeDeserializer;
import io.cloudevents.json.ZonedDateTimeSerializer;

import java.time.ZonedDateTime;

/**
 * default Jackson object mapper with CloudEvents support
 *
 * @author leijuan
 */
final class DefaultObjectMapper {
    public static final ObjectMapper OBJECT_MAPPER = initMapper();

    private DefaultObjectMapper() {
    }

    private static ObjectMapper initMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.WRAPPER_OBJECT);
        mapper.registerModule(new Jdk8Module());
        final SimpleModule module = new SimpleModule();
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        module.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        mapper.registerModule(module);
        return mapper;
    }
}