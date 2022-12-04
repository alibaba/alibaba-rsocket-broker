package com.alibaba.user;

import com.alibaba.rsocket.RSocketServiceInterface;
import com.alibaba.rsocket.ServiceMapping;
import com.alibaba.rsocket.util.ByteBufBuilder;
import io.cloudevents.CloudEvent;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * user reactive service
 *
 * @author leijuan
 */
@RSocketServiceInterface
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
        ByteBuf buf = ByteBufBuilder.builder().value(id).value(nick).build();
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
     * @param userIdFlux point
     * @return user
     */
    Flux<User> recent(Flux<Integer> userIdFlux);

    /**
     * channel but with Mono return
     *
     * @param feeds feeds
     * @return feeds count
     */
    Mono<Integer> postFeeds(Flux<String> feeds);

    Flux<User> recentWithType(String type, Flux<Integer> userIdFlux);

    Mono<String> error(String text);

    @ServiceMapping(resultEncoding = "application/octet-stream")
    Mono<ByteBuf> findAvatar(Integer id);

    @ServiceMapping(paramEncoding = "application/octet-stream", resultEncoding = "application/octet-stream")
    Mono<ByteBuf> findUserByIdAndNick(ByteBuf byteBuf);

    Mono<Void> fireLoginEvent(CloudEvent loginEvent);

    Mono<CloudEvent> processLoginEvent(CloudEvent loginEvent);
}
