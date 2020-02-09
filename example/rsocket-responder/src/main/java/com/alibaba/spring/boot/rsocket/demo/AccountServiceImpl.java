package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.account.Account;
import com.alibaba.account.AccountService;
import com.alibaba.spring.boot.rsocket.RSocketService;
import com.github.javafaker.Faker;
import com.google.protobuf.Int32Value;
import reactor.core.publisher.Mono;

/**
 * account service implementation
 *
 * @author leijuan
 */
@RSocketService(serviceInterface = AccountService.class, encoding = "protobuf")
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
}
