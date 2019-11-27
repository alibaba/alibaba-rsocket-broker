package com.alibaba.spring.boot.rsocket.broker.services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * configuration service
 *
 * @author leijuan
 */
public interface ConfigurationService {

    Flux<String> getGroups();

    Flux<String> findNamesByGroup(String groupName);

    Mono<Void> put(String key, String value);

    Mono<Void> delete(String key);

    Mono<String> get(String key);

    Flux<String> watch(String key);
}
