package com.alibaba.spring.boot.rsocket.broker.cloudevents;

import com.alibaba.rsocket.cloudevents.CloudEventsNotifyService;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import com.alibaba.spring.boot.rsocket.broker.supporting.RSocketLocalService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * CloudEvents notify service implementation
 *
 * @author leijuan
 */
@RSocketLocalService(serviceInterface = CloudEventsNotifyService.class)
public class CloudEventsNotifyServiceImpl implements CloudEventsNotifyService {
    private RSocketBrokerHandlerRegistry handlerRegistry;

    public CloudEventsNotifyServiceImpl(RSocketBrokerHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    public Mono<Void> notify(String appId, String cloudEventJson) {
        RSocketBrokerResponderHandler handler = handlerRegistry.findByUUID(appId);
        if (handler != null) {
            return handler.fireCloudEventToPeer(cloudEventJson);
        } else {
            return Mono.empty();
        }
    }

    @Override
    public Mono<Void> notifyAll(String appName, String cloudEventJson) {
        return Flux.fromIterable(handlerRegistry.findByAppName(appName))
                .flatMap(responderHandler -> responderHandler.fireCloudEventToPeer(cloudEventJson))
                .then();
    }
}
