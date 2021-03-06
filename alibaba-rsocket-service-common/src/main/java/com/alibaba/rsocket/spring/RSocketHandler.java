package com.alibaba.rsocket.spring;

import org.springframework.core.annotation.AliasFor;
import org.springframework.messaging.handler.annotation.MessageMapping;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@MessageMapping()
public @interface RSocketHandler {
    @AliasFor(annotation = MessageMapping.class)
    String[] value() default {};
}
