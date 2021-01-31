package com.alibaba.rsocket.events;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import reactor.core.publisher.Mono;

/**
 * CloudEvents Consumer
 *
 * @author leijuan
 */
public interface CloudEventsConsumer {

    boolean shouldAccept(CloudEventImpl<?> cloudEvent);

    Mono<Void> accept(CloudEventImpl<?> cloudEvent);
}
