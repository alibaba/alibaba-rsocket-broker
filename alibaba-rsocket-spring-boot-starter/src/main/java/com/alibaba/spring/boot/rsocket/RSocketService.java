package com.alibaba.spring.boot.rsocket;

import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * rsocket Service annotation
 *
 * @author leijuan
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Service
public @interface RSocketService {
    /**
     * service interface
     *
     * @return service interface
     */
    Class<?> serviceInterface();

    /**
     * service name
     *
     * @return service name
     */
    String name() default "";

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
    String[] encoding() default {"hessian", "json", "protobuf"};

    /**
     * service labels
     *
     * @return labels
     */
    String[] labels() default {};
}
