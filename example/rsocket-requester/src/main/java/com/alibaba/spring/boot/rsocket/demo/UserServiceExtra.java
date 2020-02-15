package com.alibaba.spring.boot.rsocket.demo;

import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

/**
 * user service extra
 *
 * @author leijuan
 */
public interface UserServiceExtra {

    Mono<ByteBuffer> findById(Integer id);
}
