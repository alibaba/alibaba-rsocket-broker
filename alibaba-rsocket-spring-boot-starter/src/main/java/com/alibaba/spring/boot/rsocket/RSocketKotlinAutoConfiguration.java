package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.encoding.JsonUtils;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import io.cloudevents.json.Json;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * RSocket Kotlin auto configuration
 *
 * @author leijuan
 */
@Configuration
@ConditionalOnClass(KotlinModule.class)
public class RSocketKotlinAutoConfiguration {

    @PostConstruct
    public void init() {
        //rsocket json en/decoding
        JsonUtils.objectMapper.registerModule(new KotlinModule());
        //cloud event en/decoding
        Json.MAPPER.registerModule(new KotlinModule());
    }
}
