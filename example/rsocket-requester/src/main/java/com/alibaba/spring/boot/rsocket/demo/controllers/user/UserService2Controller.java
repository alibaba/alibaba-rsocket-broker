package com.alibaba.spring.boot.rsocket.demo.controllers.user;

import com.alibaba.user.User;
import com.alibaba.user.UserService2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
public class UserService2Controller {
    @Autowired
    private UserService2 userService2;

    @GetMapping("/user2/{id}")
    public Mono<User> user(@PathVariable Integer id) {
        return userService2.findById(id);
    }

}
