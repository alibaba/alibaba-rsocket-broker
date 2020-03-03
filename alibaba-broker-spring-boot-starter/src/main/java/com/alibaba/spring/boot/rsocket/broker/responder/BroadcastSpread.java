package com.alibaba.spring.boot.rsocket.broker.responder;

import io.cloudevents.v1.CloudEventImpl;
import reactor.core.publisher.Mono;

/**
 * broadcast spread for cloudevents
 *
 * @author leijuan
 */
public interface BroadcastSpread {

    Mono<Void> send(String appUUID, final CloudEventImpl cloudEvent);

    Mono<Void> broadcast(String appName, final CloudEventImpl cloudEvent);

}
