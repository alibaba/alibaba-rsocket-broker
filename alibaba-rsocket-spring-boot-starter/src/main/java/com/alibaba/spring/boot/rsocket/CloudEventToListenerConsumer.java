package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.events.CloudEventsConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

/**
 * CloudEvent to @EventListener handler
 *
 * @author leijuan
 */
public class CloudEventToListenerConsumer implements CloudEventsConsumer {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public boolean shouldAccept(CloudEventImpl<?> cloudEvent) {
        return true;
    }

    @Override
    public Mono<Void> accept(CloudEventImpl<?> cloudEvent) {
        return Mono.fromRunnable(() -> {
            eventPublisher.publishEvent(cloudEvent);
        });
    }
}
