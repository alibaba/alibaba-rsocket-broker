package com.alibaba.rsocket.spring;

import org.springframework.core.annotation.AliasFor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@MessageMapping()
public @interface SpringRSocketService {
    @AliasFor(annotation = MessageMapping.class)
    String[] value() default {};
}
