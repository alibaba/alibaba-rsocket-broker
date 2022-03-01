package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.user.Account;
import com.alibaba.user.AccountService;
import com.alibaba.rsocket.RSocketService;
import com.google.protobuf.Int32Value;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Random;

/**
 * account service implementation
 *
 * @author leijuan
 */
@RSocketService(serviceInterface = AccountService.class, encoding = "protobuf")
@Service
public class AccountServiceImpl implements AccountService {
    private Faker faker = new Faker();

    @Override
    public Mono<Account> findById(Int32Value id) {
        return Mono.just(Account.newBuilder().setId(id.getValue())
                .setEmail(faker.internet().emailAddress())
                .setPhone(faker.phoneNumber().cellPhone())
                .setNick(faker.name().name())
                .build());
    }

    @Override
    public Flux<Account> findByStatus(Int32Value status) {
        return Flux.just(Account.newBuilder().setId(new Random().nextInt())
                        .setEmail(faker.internet().emailAddress())
                        .setPhone(faker.phoneNumber().cellPhone())
                        .setNick(faker.name().name())
                        .setStatus(status.getValue())
                        .build(),
                Account.newBuilder().setId(new Random().nextInt())
                        .setEmail(faker.internet().emailAddress())
                        .setPhone(faker.phoneNumber().cellPhone())
                        .setNick(faker.name().name())
                        .setStatus(status.getValue())
                        .build()
        );
    }

    @Override
    public Flux<Account> findByIdStream(Flux<Int32Value> idStream) {
        return idStream.map(id -> Account.newBuilder()
                .setId(id.getValue())
                .setEmail(faker.internet().emailAddress())
                .setPhone(faker.phoneNumber().cellPhone())
                .setNick(faker.name().name())
                .setStatus(1)
                .build());
    }
}
