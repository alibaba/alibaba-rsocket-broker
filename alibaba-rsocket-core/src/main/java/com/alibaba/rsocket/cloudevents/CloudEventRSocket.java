package com.alibaba.rsocket.cloudevents;

import io.cloudevents.CloudEvent;
import io.cloudevents.v1.CloudEventImpl;
import io.rsocket.RSocket;
import reactor.core.publisher.Mono;

/**
 * RSocket with CloudEvents support
 *
 * @author leijuan
 */
public interface CloudEventRSocket extends RSocket {

    Mono<Void> fireCloudEvent(CloudEventImpl<?> cloudEvent);
}
