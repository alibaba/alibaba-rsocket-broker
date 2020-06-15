package com.alibaba.rsocket;

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
    String[] encoding() default {"hessian", "json", "protobuf"};

    /**
     * service tags
     *
     * @return labels
     */
    String[] tags() default {};
}
