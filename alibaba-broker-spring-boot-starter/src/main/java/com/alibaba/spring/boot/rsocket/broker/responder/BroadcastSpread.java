package com.alibaba.spring.boot.rsocket.broker.responder;

import com.alibaba.rsocket.RSocketAppContext;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * broadcast spread for cloudevents
 *
 * @author leijuan
 */
public interface BroadcastSpread {

    Mono<Void> send(@NotNull String appUUID, final CloudEventImpl cloudEvent);

    Mono<Void> broadcast(@NotNull String appName, final CloudEventImpl cloudEvent);

    Mono<Void> broadcastAll(CloudEventImpl cloudEvent);

    default CloudEventImpl<Map<String, Object>> buildMapCloudEvent(@NotNull String type, @NotNull String subject, @NotNull Map<String, Object> data) {
        return CloudEventBuilder.<Map<String, Object>>builder()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("broker://" + RSocketAppContext.ID))
                .withType(type)
                .withTime(ZonedDateTime.now())
                .withDataContentType("application/json")
                .withData(data)
                .withSubject(subject)
                .build();
    }
}
