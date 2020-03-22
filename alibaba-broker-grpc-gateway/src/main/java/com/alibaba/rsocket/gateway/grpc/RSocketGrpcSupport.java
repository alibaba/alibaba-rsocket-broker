package com.alibaba.rsocket.gateway.grpc;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * RSocket gRPC Support
 *
 * @author leijuan
 */
public interface RSocketGrpcSupport {

    default <T> Mono<T> rsocketRpc(RSocket rsocket, Payload requestPayload, final Class<T> responseClass) {
        return rsocket.requestResponse(requestPayload)
                .handle((payload, sink) -> {
                    try {
                        sink.next(PayloadUtils.payloadToResponseObject(payload, responseClass));
                    } catch (Exception e) {
                        sink.error(e);
                    }
                });
    }

    default <T> Flux<T> rsocketStream(RSocket rsocket, Payload requestPayload, final Class<T> responseClass) {
        return rsocket.requestStream(requestPayload)
                .handle((payload, sink) -> {
                    try {
                        sink.next(PayloadUtils.payloadToResponseObject(payload, responseClass));
                    } catch (Exception e) {
                        sink.error(e);
                    }
                });
    }

    default <T> Flux<T> rsocketChannel(RSocket rsocket, Flux<Payload> requestPayload, final Class<T> responseClass) {
        return rsocket.requestChannel(requestPayload)
                .handle((payload, sink) -> {
                    try {
                        sink.next(PayloadUtils.payloadToResponseObject(payload, responseClass));
                    } catch (Exception e) {
                        sink.error(e);
                    }
                });
    }

}
