package com.alibaba.spring.boot.rsocket.broker.responder;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;
import com.alibaba.spring.boot.rsocket.broker.BrokerAppContext;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * broadcast spread for cloudevents
 *
 * @author leijuan
 */
@SuppressWarnings("rawtypes")
public interface BroadcastSpread {

    Mono<Void> send(@NotNull String appUUID, final CloudEventImpl cloudEvent);

    Mono<Void> broadcast(@NotNull String appName, final CloudEventImpl cloudEvent);

    Mono<Void> broadcastAll(CloudEventImpl cloudEvent);

    default CloudEventImpl<Map<String, Object>> buildMapCloudEvent(@NotNull String type, @NotNull String subject, @NotNull Map<String, Object> data) {
        return RSocketCloudEventBuilder.builder(data)
                .withSource(BrokerAppContext.identity())
                .withType(type)
                .withSubject(subject)
                .build();
    }
}
