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

    /**
     * service interface
     *
     * @return service interface
     */
    Class<?> serviceInterface() default Void.class;

    /**
     * service group
     *
     * @return group
     */
    String group() default "";

    /**
     * service version
     *
     * @return version
     */
    String version() default "";

    /**
     * encoding strategies
     *
     * @return encoding names
     */
    String[] encoding() default {"json", "cbor"};

    /**
     * service tags
     *
     * @return labels
     */
    String[] tags() default {};
}
