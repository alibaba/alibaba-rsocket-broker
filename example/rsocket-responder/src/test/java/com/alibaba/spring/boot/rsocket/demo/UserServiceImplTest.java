package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


public class UserServiceImplTest extends SpringBootBaseTest {
    @Autowired
    private UserService userService;

    @Test
    public void testFindById() {
        System.out.println(userService.findById(1).block());
    }
}
