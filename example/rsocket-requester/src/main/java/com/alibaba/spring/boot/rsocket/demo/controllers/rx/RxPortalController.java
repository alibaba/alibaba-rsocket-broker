package com.alibaba.spring.boot.rsocket.demo.controllers.rx;

import com.alibaba.user.RxUserService;
import com.alibaba.user.User;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * rx portal demo for test
 *
 * @author leijuan
 */
@RestController
@RequestMapping("/rx")
public class RxPortalController {
    @Autowired
    RxUserService rxUserService;

    @RequestMapping("/users")
    public Single<List<User>> all() {
        return rxUserService.findAll();
    }

    @GetMapping("/user/{id}")
    public Maybe<User> user(@PathVariable Integer id) {
        return rxUserService.findById(id);
    }

    @RequestMapping("/")
    public String index() {
        return "welcome";
    }
}
