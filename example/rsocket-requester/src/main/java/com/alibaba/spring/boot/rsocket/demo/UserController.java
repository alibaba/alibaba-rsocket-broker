package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.user.User;
import com.alibaba.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * User Controller
 *
 * @author leijuan
 */
@RestController
public class UserController {
    @Autowired
    private UserService userService;

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

    @GetMapping("/bytebuf/user/{id}")
    public Mono<User> byteBufUser(@PathVariable Integer id) {
        return userService.findByIdOrNick(id, "Fake nick");
    }

}
