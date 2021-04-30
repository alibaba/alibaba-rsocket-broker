package com.alibaba.spring.boot.rsocket.demo.controllers.protobuf;

import com.alibaba.user.Account;
import com.alibaba.user.AccountService;
import com.google.protobuf.Int32Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * account controller
 *
 * @author leijuan
 */
@RestController
@RequestMapping("/account")
public class AccountController {
    @Autowired
    private AccountService accountService;

    @GetMapping("/{id}")
    public Mono<Account> account(@PathVariable Integer id) {
        return accountService.findById(Int32Value.of(id));
    }

}
