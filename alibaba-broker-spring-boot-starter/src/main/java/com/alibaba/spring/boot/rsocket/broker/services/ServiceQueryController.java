package com.alibaba.spring.boot.rsocket.broker.services;

import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
                .filter(s -> s.contains(":" + serviceName + ":"))
                .map(s -> {
                    Map<String, Object> serviceInfo = new HashMap<>();
                    //todo 调整逻辑
                    Set<String> appInstances = Collections.emptySet(); //serviceRoutingSelector.getAppInstances(s);
                    Set<Map<String, Object>> appList = appInstances.stream()
                            .map(appId -> handlerRegistry.findByUUID(appId))
                            .map(handler -> {
                                AppMetadata appMetadata = handler.getAppMetadata();
                                Map<String, Object> app = new HashMap<>();
                                app.put("ip", appMetadata.getIp());
                                return app;
                            }).collect(Collectors.toSet());
                    serviceInfo.put("apps", appList);
                    return serviceInfo;
                });
    }

}
