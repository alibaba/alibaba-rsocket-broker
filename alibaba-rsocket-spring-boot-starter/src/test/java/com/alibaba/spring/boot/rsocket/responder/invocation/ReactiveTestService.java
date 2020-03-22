package com.alibaba.spring.boot.rsocket.responder.invocation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * reactive service for test
 *
 * @author leijuan
 */
public interface ReactiveTestService {

    Mono<String> findNickById(Integer id);

    Flux<String> findNames();

    Flux<String> requestGlobals(Flux<String> actions);

    Flux<String> requestRecommendations(String level, Flux<String> actions);
}
