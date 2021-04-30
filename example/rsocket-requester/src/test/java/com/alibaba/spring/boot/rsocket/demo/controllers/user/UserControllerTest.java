package com.alibaba.spring.boot.rsocket.demo.controllers.user;

import com.alibaba.spring.boot.rsocket.demo.SpringBootBaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


public class UserControllerTest extends SpringBootBaseTest {
    @Autowired
    private UserController userController;

    @Test
    public void testFindUserById() {
        System.out.println(userController.getClass());
    }
}
