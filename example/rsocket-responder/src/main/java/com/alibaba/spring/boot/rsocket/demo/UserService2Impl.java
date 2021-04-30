package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.RSocketService;
import com.alibaba.user.User;
import com.alibaba.user.UserService2;
import com.github.javafaker.Faker;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;


@RSocketService(serviceInterface = UserService2.class)
@Service
public class UserService2Impl implements UserService2 {
    private Faker faker = new Faker();

    @Override
    public Mono<User> findById(Integer id) {
        return Mono.just(randomUser(id));
    }

    private User randomUser(@Nullable Integer id) {
        User user = new User();
        user.setId(id == null ? new Random().nextInt() : id);
        user.setNick(faker.name().name());
        user.setPhone(faker.phoneNumber().cellPhone());
        user.setEmail(faker.internet().emailAddress());
        user.setCreatedAt(new Date());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
