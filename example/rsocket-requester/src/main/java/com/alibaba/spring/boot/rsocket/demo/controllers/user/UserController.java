package com.alibaba.spring.boot.rsocket.demo.controllers.user;

import com.alibaba.rsocket.util.ByteBufBuilder;
import com.alibaba.user.User;
import com.alibaba.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.netty.buffer.ByteBuf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * User Controller
 *
 * @author leijuan
 */
@RestController
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private ObjectMapper objectMapper;

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

    @GetMapping(value = "/user/bytebuf", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> bytebuf2() {
        ByteBuf content = ByteBufBuilder.builder().value(1).value("Jackie").build();
        return userService.findUserByIdAndNick(content).map(byteBuf -> byteBuf.toString(StandardCharsets.UTF_8));
    }

    @GetMapping("/user/requester/{id}")
    public Mono<User> findByIdNative(@PathVariable Integer id) {
        return requester.route("com.alibaba.user.UserService.findById").data(id).retrieveMono(User.class);
        // return requester.route("com.alibaba.user.UserService.findByEmailOrPhone").data(new Object[]{"libing.chen@gmail.com", "18888"}).retrieveMono(User.class);
    }

    @GetMapping("/user/cloudevents")
    public Mono<String> cloudEvent() throws Exception {
        User user = new User(1, "leijuan");
        CloudEvent event = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("https://example.com/users"))
                .withType("com.alibaba.user.User")
                .withData("application/json", objectMapper.writeValueAsBytes(user)) //
                .build();
        return userService.processLoginEvent(event).map(cloudEvent -> cloudEvent.getId());
    }
}
