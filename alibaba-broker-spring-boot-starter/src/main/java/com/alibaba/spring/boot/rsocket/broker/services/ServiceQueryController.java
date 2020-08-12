package com.alibaba.spring.boot.rsocket.broker.services;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.rpc.ReactiveServiceDiscovery;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
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
    private static MessageMimeTypeMetadata jsonMetaEncoding = new MessageMimeTypeMetadata(RSocketMimeType.Json);
    @Autowired
    private ServiceRoutingSelector routingSelector;
    @Autowired
    private RSocketBrokerHandlerRegistry brokerHandlerRegistry;

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

    @GetMapping(value = "/definition/{serviceName}")
    public Mono<String> queryDefinition(@PathVariable(name = "serviceName") String serviceName) {
        Integer handler = routingSelector.findHandler(new ServiceLocator("", serviceName, "").getId());
        if (handler != null) {
            RSocketBrokerResponderHandler brokerResponderHandler = brokerHandlerRegistry.findById(handler);
            if (brokerResponderHandler != null) {
                GSVRoutingMetadata routingMetadata = new GSVRoutingMetadata("", ReactiveServiceDiscovery.class.getCanonicalName() + ".findServiceByFullName", "");
                RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(routingMetadata, jsonMetaEncoding);
                ByteBuf bodyBuf = Unpooled.wrappedBuffer(("[\"" + serviceName + "\"]").getBytes(StandardCharsets.UTF_8));
                return brokerResponderHandler.getPeerRsocket()
                        .requestResponse(ByteBufPayload.create(bodyBuf, compositeMetadata.getContent()))
                        .map(Payload::getDataUtf8);
            }
        }
        return Mono.error(new Exception(RsocketErrorCode.message("RST-900404", serviceName)));
    }

}
