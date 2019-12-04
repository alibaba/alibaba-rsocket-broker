package com.alibaba.rsocket.health;

import reactor.core.publisher.Mono;

/**
 * health check for rsocket service
 *
 * @author leijuan
 */
public interface RSocketServiceHealth {
    /**
     * health status: 0:unknown, 1: serving, -1: out of service
     *
     * @param serviceName service name
     * @return health status
     */
    Mono<Integer> check(String serviceName);
}
