package com.alibaba.spring.boot.rsocket.broker.supporting;

import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * rsocket local Service annotation for broker
 *
 * @author leijuan
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Service
public @interface RSocketLocalService {
    /**
     * service interface
     *
     * @return service interface
     */
    Class serviceInterface();

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
     * service catalogs, almost like tags
     *
     * @return catalogs
     */
    String[] catalogs() default {};
}
