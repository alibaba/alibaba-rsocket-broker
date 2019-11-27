package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.user.User;
import com.alibaba.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * portal demo for test
 *
 * @author leijuan
 */
@RestController
public class PortalController {
    @Autowired
    UserService userService;

    @RequestMapping("/users")
    public Mono<List<User>> all() {
        return userService.findAll();
    }

    @PostMapping("/user/save")
    public Mono<Integer> save(@RequestBody User user) {
        return userService.save(user);
    }

    @GetMapping("/user/{id}")
    public Mono<User> user(@PathVariable Integer id) {
        return userService.findById(id);
    }

    @RequestMapping("/appName")
    public Mono<String> appName() {
        return userService.getAppName();
    }

    @RequestMapping("/flux")
    public Flux<User> flux() {
        return userService.findAllPeople("vip");
    }

    @RequestMapping("/channel")
    public Flux<User> channel() {
        Flux<Date> dates = Flux.interval(Duration.ofMillis(1000))
                .map(Date::new);
        return userService.recent(dates);
    }

    @RequestMapping("/")
    public String index() {
        return "welcome";
    }
}
