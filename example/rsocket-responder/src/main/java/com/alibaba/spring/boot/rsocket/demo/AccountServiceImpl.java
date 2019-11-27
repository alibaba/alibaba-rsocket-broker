package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.account.Account;
import com.alibaba.account.AccountService;
import com.alibaba.spring.boot.rsocket.RSocketService;
import com.google.protobuf.Int32Value;
import reactor.core.publisher.Mono;

/**
 * account service implementation
 *
 * @author leijuan
 */
@RSocketService(serviceInterface = AccountService.class, encoding = "protobuf")
public class AccountServiceImpl implements AccountService {
    @Override
    public Mono<Account> findById(Int32Value id) {
        return Mono.fromCallable(() -> Account.newBuilder()
                .setId(id.getValue())
                .setEmail("demo@demo.com")
                .setPhone("1860000000")
                .build()
        );
    }
}
