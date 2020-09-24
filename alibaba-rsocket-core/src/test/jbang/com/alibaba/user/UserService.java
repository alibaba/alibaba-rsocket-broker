package com.alibaba.user;

import reactor.core.publisher.Mono;

public interface UserService {
    Mono<User> findById(Integer id);

    Mono<String> getAppName();
}
