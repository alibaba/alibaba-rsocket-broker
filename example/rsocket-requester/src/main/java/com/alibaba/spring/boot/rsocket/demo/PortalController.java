package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.user.User;
import com.alibaba.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * portal demo for test
 *
 * @author leijuan
 */
@RestController
public class PortalController {
    @Autowired
    UserService userService;

    @GetMapping("/error")
    public Mono<String> error() {
        return userService.error("Hi");
    }

    @GetMapping("/job1")
    public Mono<Void> job1() {
        return userService.job1();
    }

    @RequestMapping("/appName")
    public Mono<String> appName() {
        return userService.getAppName();
    }

    @RequestMapping("/flux")
    public Flux<User> flux() {
        return userService.findAllPeople("vip");
    }

    @RequestMapping("/channel1")
    public Flux<User> channel1() {
        Flux<Integer> userIdFlux = Flux.range(1, 20);
        return userService.recent(userIdFlux);
    }

    @RequestMapping("/channel2")
    public Flux<User> channel2() {
        Flux<Integer> userIdFlux = Flux.range(1, 20);
        return userService.recentWithType("VIP", userIdFlux);
    }

    @RequestMapping("/monoChannel")
    public Mono<Integer> monoChannel() {
        return userService.postFeeds(Flux.just("one", "two", "three"));
    }

    @RequestMapping("/")
    public String index() {
        return "This is RSocket Requester App!";
    }
}
