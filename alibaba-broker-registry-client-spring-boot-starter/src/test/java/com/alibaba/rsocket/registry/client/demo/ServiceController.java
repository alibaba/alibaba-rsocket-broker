package com.alibaba.rsocket.registry.client.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Flux;

/**
 * service controller
 *
 * @author leijuan
 */
@RestController
public class ServiceController {

    @Autowired
    private DiscoveryClient discoveryClient;

    @RequestMapping("/server/{name}")
    public Flux<String> serverInstances(@PathVariable(name = "name") String name) {
        return Flux.fromIterable(discoveryClient.getInstances(HtmlUtils.htmlEscape(name)))
                .map(serviceInstance -> serviceInstance.getHost() + ":" + serviceInstance.getPort());
    }
}
