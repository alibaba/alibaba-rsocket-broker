package com.alibaba.spring.boot.rsocket.demo.controllers.rx;

import com.alibaba.user.Rx3UserService;
import com.alibaba.user.User;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableConverter;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public Maybe<User> user(@PathVariable Integer id) {
        return rx3UserService.findById(id);
    }

    @RequestMapping("/allPeople")
    public Flowable<User> allPeople() {
        return rx3UserService.findAllPeople("vip");
    }

    @RequestMapping("/channel1")
    public Observable<User> index() {
        return rx3UserService.channel1(Observable.fromArray(1, 2));
    }
}
