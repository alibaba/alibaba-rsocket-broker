package com.alibaba.rsocket.listen;

/**
 * RSocket listener customizer
 *
 * @author leijuan
 */
@FunctionalInterface
public interface RSocketListenerCustomizer {

    void customize(RSocketListener.Builder builder);

}
