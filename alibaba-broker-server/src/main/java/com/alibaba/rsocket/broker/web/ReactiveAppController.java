package com.alibaba.rsocket.broker.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * reactive app controller
 *
 * @author leijuan
 */
@Controller
@RequestMapping("/app")
public class ReactiveAppController {
    @RequestMapping("/index")
    public String index() {
        return "app/index";
    }
}
