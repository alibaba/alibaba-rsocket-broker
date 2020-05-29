package com.alibaba.rsocket.rpc;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Demo reactive service
 *
 * @author leijuan
 */
public interface DemoReactiveService {
    Mono<String> findNickById(int id);

    Flux<String> findNicks(Flux<Integer> ids);

    @Deprecated
    Mono<String> findNickByEmail(String email);
}
