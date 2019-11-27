package com.alibaba.rsocket.broker.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * reactive service controller
 *
 * @author leijuan
 */
@Controller
@RequestMapping("/service")
public class ReactiveServiceController {
    @RequestMapping("/index")
    public String index() {
        return "service/index";
    }
}
