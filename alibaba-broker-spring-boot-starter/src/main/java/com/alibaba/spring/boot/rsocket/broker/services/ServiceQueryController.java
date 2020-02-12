package com.alibaba.spring.boot.rsocket.broker.services;

import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * service query controller
 *
 * @author leijuan
 */
@RestController
@RequestMapping("/service")
public class ServiceQueryController {
    @Autowired
    private ServiceRoutingSelector routingSelector;
    @Autowired
    private RSocketBrokerHandlerRegistry handlerRegistry;

    @GetMapping("/{serviceName}")
    public Flux<Map<String, Object>> query(@PathVariable(name = "serviceName") String serviceName) {
        return Flux.fromIterable(routingSelector.findAllServices())
                .filter(locator -> locator.getService().equals(serviceName))
                .map(locator -> {
                    Map<String, Object> serviceInfo = new HashMap<>();
                    serviceInfo.put("count", routingSelector.getInstanceCount(locator.getId()));
                    if (locator.getGroup() != null) {
                        serviceInfo.put("group", locator.getGroup());
                    }
                    if (locator.getVersion() != null) {
                        serviceInfo.put("version", locator.getVersion());
                    }
                    return serviceInfo;
                });
    }

}
