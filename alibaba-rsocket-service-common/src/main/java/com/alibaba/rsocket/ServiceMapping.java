package com.alibaba.rsocket;

import java.lang.annotation.*;

/**
 * Service Mapping for reactive interface method
 *
 * @author leijuan
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceMapping {
    /**
     * service mapping value, service or handler name
     *
     * @return mapping value
     */
    String value() default "";

    String group() default "";

    String version() default "";

    /**
     * params encoding with MIME Type
     *
     * @return mime type
     */
    String paramEncoding() default "";

    /**
     * result value encoding with MIME Type
     *
     * @return mime type
     */
    String resultEncoding() default "";

    /**
     * endpoint, such as id:xxxx,  ip:192.168.1.2
     *
     * @return endpoint
     */
    String endpoint() default "";

    /**
     * sticky session
     *
     * @return sticky or not
     */
    boolean sticky() default false;
}
