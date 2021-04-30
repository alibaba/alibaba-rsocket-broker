package com.alibaba.user;

import reactor.core.publisher.Mono;

/**
 * user reactive service 2 for benchmark test
 *
 * @author leijuan
 */
public interface UserService2 {
    /**
     * RPC call to get user
     *
     * @param id user
     * @return user
     */
    Mono<User> findById(Integer id);
}
