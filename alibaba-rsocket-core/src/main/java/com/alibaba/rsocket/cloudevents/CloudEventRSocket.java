package com.alibaba.rsocket.cloudevents;

import io.cloudevents.json.Json;
import io.cloudevents.v1.CloudEventImpl;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Mono;

/**
 * RSocket with CloudEvents support
 *
 * @author leijuan
 */
public interface CloudEventRSocket extends RSocket {

    Mono<Void> fireCloudEvent(CloudEventImpl<?> cloudEvent);

    default Payload cloudEventToMetadataPushPayload(CloudEventImpl<?> cloudEvent) {
        return ByteBufPayload.create(Unpooled.EMPTY_BUFFER, Unpooled.wrappedBuffer(Json.binaryEncode(cloudEvent)));
    }
}
