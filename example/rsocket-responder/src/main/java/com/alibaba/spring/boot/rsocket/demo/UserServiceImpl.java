package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.RSocketService;
import com.alibaba.rsocket.util.ByteBufTuples;
import com.alibaba.user.User;
import com.alibaba.user.UserService;
import com.github.javafaker.Faker;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * user service implementation
 *
 * @author leijuan
 */
@RSocketService(serviceInterface = UserService.class)
@Service
public class UserServiceImpl implements UserService {
    private Faker faker = new Faker();

    @Override
    public Mono<User> findById(Integer id) {
        return Mono.just(randomUser(id));
    }

    @Override
    public Mono<User> findByEmailOrPhone(String email, String phone) {
        return Mono.fromCallable(() -> randomUser(null));
    }

    @Override
    public Mono<User> _findByIdOrNick(ByteBuf byteBuf) {
        Tuple2<Integer, String> tuple2 = ByteBufTuples.of(byteBuf, Integer.class, String.class);
        User user = new User();
        user.setId(tuple2.getT1());
        user.setNick(tuple2.getT2());
        user.setEmail(faker.internet().emailAddress());
        user.setPhone(faker.phoneNumber().phoneNumber());
        return Mono.just(user);
    }

    @Override
    public Mono<List<User>> findAll() {
        return Mono.just(Arrays.asList(randomUser(null), randomUser(null)));
    }

    @Override
    public Mono<Integer> save(User user) {
        return Mono.just(RandomUtils.nextInt());
    }

    @Override
    public Mono<Void> flush(String name) {
        System.out.println("flush");
        return Mono.empty();
    }

    @Override
    public Mono<String> getAppName() {
        return Mono.just("UserService");
    }

    @Override
    public Mono<Void> job1() {
        System.out.println("job1");
        return Mono.empty();
    }

    @Override
    public Flux<User> findAllPeople(String type) {
        return Flux.range(0, 10)
                .map(id -> new User(id, "nick:" + type));
    }

    @Override
    public Flux<User> recent(Flux<Date> point) {
        point.subscribe(t -> {
            System.out.println("time:" + point);
        });
        return Flux.interval(Duration.ofMillis(1000))
                .map(timestamp -> new User((int) (timestamp % 1000), "nick"));
    }

    @Override
    public Mono<String> error(String text) {
        return Mono.error(new Exception("this is an Exception!"));
    }

    private User randomUser(@Nullable Integer id) {
        User user = new User();
        user.setId(id == null ? new Random().nextInt() : id);
        user.setNick(faker.name().name());
        user.setPhone(faker.phoneNumber().cellPhone());
        user.setEmail(faker.internet().emailAddress());
        return user;
    }
}
