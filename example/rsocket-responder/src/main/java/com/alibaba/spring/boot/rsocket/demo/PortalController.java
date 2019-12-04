package com.alibaba.spring.boot.rsocket.demo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RSocket Responder portal controller
 *
 * @author leijuan
 */
@RestController
public class PortalController {
    @RequestMapping("/")
    public String index() {
        return "This is RSocket Responder Server!";
    }
}
