package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.reactive.rxjava3.RxJava3Adapter;
import com.alibaba.user.Rx3UserService;
import com.alibaba.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * rx3 portal demo for test
 *
 * @author leijuan
 */
@RestController
@RequestMapping("/rx3")
public class Rx3PortalController {
    @Autowired
    Rx3UserService rx3UserService;

    @GetMapping("/user/{id}")
    public Mono<User> user(@PathVariable Integer id) {
        return RxJava3Adapter.maybeToMono(rx3UserService.findById(id));
    }

    @RequestMapping("/")
    public String index() {
        return "welcome";
    }
}
