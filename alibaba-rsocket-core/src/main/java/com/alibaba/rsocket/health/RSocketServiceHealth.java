package com.alibaba.rsocket.health;

import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

/**
 * health check for rsocket service
 *
 * @author leijuan
 */
@FunctionalInterface
public interface RSocketServiceHealth {
    int SERVING_STATUS = 1;
    int UNKNOWN_STATUS = 0;
    int DOWN_STATUS = -1;

    /**
     * health status: 0:unknown, 1: serving, -1: out of service
     *
     * @param serviceName service name. if service name is null, just check server's health
     * @return health status
     */
    Mono<Integer> check(@Nullable String serviceName);
}
