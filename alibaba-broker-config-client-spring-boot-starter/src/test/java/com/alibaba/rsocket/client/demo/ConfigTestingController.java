package com.alibaba.rsocket.client.demo;

import com.alibaba.rsocket.config.bootstrap.RSocketConfigPropertySourceLocator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static org.springframework.context.annotation.ScopedProxyMode.DEFAULT;

/**
 * config testing controller
 *
 * @author leijuan
 */
@RestController
@RefreshScope(proxyMode = DEFAULT)
public class ConfigTestingController {
    @Value("${developer:unknown}")
    private String developer;

    @RequestMapping("/config/display")
    public Mono<String> serverInstances() {
        return Mono.just(RSocketConfigPropertySourceLocator.getLastConfigText());
    }

    @RequestMapping("/config/developer")
    private Mono<String> developer() {
        return Mono.just(developer);
    }
}
