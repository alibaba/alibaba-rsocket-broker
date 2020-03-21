package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.util.ByteBufBuilder;
import com.alibaba.user.User;
import com.alibaba.user.UserService;
import io.netty.buffer.ByteBuf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
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

    @Autowired(required = false)
    private RSocketRequester requester;

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

    @GetMapping("/user/default/{id}")
    public Mono<User> userFromDefaultMethod(@PathVariable Integer id) {
        return userService.findByIdFromDefault(id);
    }

    @GetMapping("/bytebuf/user/{id}")
    public Mono<User> byteBufUser(@PathVariable Integer id) {
        return userService.findByIdOrNick(id, "Fake nick");
    }

    @GetMapping("/user/avatar")
    public Mono<String> avatar() {
        return userService.findAvatar(1).map(byteBuf -> byteBuf.toString(StandardCharsets.UTF_8));
    }

    @GetMapping("/user/bytebuf")
    public Mono<String> bytebuf2() {
        ByteBuf content = ByteBufBuilder.builder().value(1).value("Jackie").build();
        return userService.findUserByIdAndNick(content).map(byteBuf -> byteBuf.toString(StandardCharsets.UTF_8));
    }

    @GetMapping("/user/requester/{id}")
    public Mono<User> findByIdNative(@PathVariable Integer id) {
        return requester.route("com.alibaba.user.UserService.findById").data(id).retrieveMono(User.class);
        // return requester.route("com.alibaba.user.UserService.findByEmailOrPhone").data(new Object[]{"libing.chen@gmail.com", "18888"}).retrieveMono(User.class);
    }
}
