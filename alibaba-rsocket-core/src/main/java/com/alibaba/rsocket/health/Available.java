package com.alibaba.rsocket.health;

import reactor.core.publisher.Mono;

/**
 * Service Available interface
 *
 * @author leijuan
 */
public interface Available {

    Mono<Boolean> isAlive();
}
