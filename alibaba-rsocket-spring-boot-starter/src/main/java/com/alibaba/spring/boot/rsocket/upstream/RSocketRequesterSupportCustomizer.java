package com.alibaba.spring.boot.rsocket.upstream;

/**
 * rsocket requester support customizer
 *
 * @author leijuan
 */
@FunctionalInterface
public interface RSocketRequesterSupportCustomizer {

    void customize(RSocketRequesterSupportBuilder builder);
}
