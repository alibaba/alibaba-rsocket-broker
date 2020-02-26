package com.alibaba.user;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.cache.annotation.CacheResult;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * user reactive service
 *
 * @author leijuan
 */
public interface UserService {
    String cacheName = "com.alibaba.user.UserService";

    /**
     * RPC call to get user
     *
     * @param id user
     * @return user
     */
    Mono<User> findById(Integer id);

    default Mono<User> findByIdFromDefault(Integer id) {
        return findById(id);
    }

    default Mono<User> findByIdOrNick(Integer id, String nick) {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(nick);
        int capacity = 4 + 4 + buffer.limit();
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer(capacity, capacity);
        buf.writeInt(id);
        buf.writeInt(buffer.limit());
        buf.writeBytes(buffer);
        return _findByIdOrNick(buf);
    }

    Mono<User> _findByIdOrNick(ByteBuf byteBuf);

    /**
     * find by email or phone
     *
     * @param email email or phone
     * @param phone phone
     * @return user
     */
    Mono<User> findByEmailOrPhone(String email, String phone);

    /**
     * find all to test list
     *
     * @return user list
     */
    Mono<List<User>> findAll();

    /**
     * save or update user
     *
     * @param user user
     * @return user's id
     */
    Mono<Integer> save(User user);

    /**
     * RPC call without parameters
     *
     * @return result
     */
    @CacheResult(cacheName = cacheName)
    Mono<String> getAppName();

    /**
     * rpc call, you want to deal success result: result.doOnSuccess(s -> { }).subscribe();
     *
     * @return Mono void
     */
    Mono<Void> job1();

    /**
     * fire & forget operation
     *
     * @param name name
     */
    Mono<Void> flush(String name);

    /**
     * request/stream to get people by type
     *
     * @param type type
     * @return user stream
     */
    Flux<User> findAllPeople(String type);

    /**
     * channel(bi-direction stream)
     *
     * @param point point
     * @return user
     */
    Flux<User> recent(Flux<Date> point);

    Mono<String> error(String text);

}
