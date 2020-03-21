package com.alibaba.spring.boot.rsocket.broker.services;

import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * App query controller
 *
 * @author leijuan
 */
@RestController
@RequestMapping("/app")
public class AppQueryController {
    @Autowired
    private RSocketBrokerHandlerRegistry handlerRegistry;

    @GetMapping("/{appName}")
    public Flux<Map<String, Object>> query(@PathVariable(name = "appName") String appName) {
        List<Map<String, Object>> apps = new ArrayList<>();
        Collection<RSocketBrokerResponderHandler> handlers = handlerRegistry.findByAppName(appName);
        if (handlers != null) {
            for (RSocketBrokerResponderHandler handler : handlers) {
                Map<String, Object> app = new HashMap<>();
                AppMetadata appMetadata = handler.getAppMetadata();
                app.put("ip", appMetadata.getIp());
                app.put("uuid", appMetadata.getUuid());
                app.put("startedAt", appMetadata.getConnectedAt());
                apps.add(app);
            }
        }
        return Flux.fromIterable(apps);
    }
}
