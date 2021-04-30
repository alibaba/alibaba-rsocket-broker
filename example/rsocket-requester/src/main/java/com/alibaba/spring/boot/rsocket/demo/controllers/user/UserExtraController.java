package com.alibaba.spring.boot.rsocket.demo.controllers.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

/**
 * user extra controller
 *
 * @author leijuan
 */
@RestController
@RequestMapping("/user/extra")
public class UserExtraController {
    @Autowired
    private UserServiceExtra userServiceExtra;


    @GetMapping(value = "/{id}", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public Mono<ByteBuffer> user(@PathVariable Integer id) {
        return userServiceExtra.findById(id);
    }

}
