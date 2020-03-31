package com.alibaba.rsocket.listen;

import com.alibaba.rsocket.encoding.RSocketEncodingFacade;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageAcceptMimeTypesMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.rpc.ReactiveMethodHandler;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.exceptions.InvalidException;
import io.rsocket.util.ByteBufPayload;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * rsocket responder support for both sides, supply some base api
 *
 * @author leijuan
 */
public abstract class RSocketResponderSupport extends AbstractRSocket {
    protected Logger log = LoggerFactory.getLogger(this.getClass());
    protected LocalReactiveServiceCaller localServiceCaller;
    protected RSocketEncodingFacade encodingFacade = RSocketEncodingFacade.getInstance();

    protected Mono<Payload> localRequestResponse(GSVRoutingMetadata routing,
                                                 MessageMimeTypeMetadata dataEncodingMetadata,
                                                 @Nullable MessageAcceptMimeTypesMetadata messageAcceptMimeTypesMetadata,
                                                 Payload payload) {
        try {
            ReactiveMethodHandler methodHandler = localServiceCaller.getInvokeMethod(routing.getService(), routing.getMethod());
            if (methodHandler != null) {
                Object result;
                if (methodHandler.isAsyncReturn()) {
                    result = invokeLocalService(methodHandler, dataEncodingMetadata, payload);
                } else {
                    result = Mono.fromCallable(() -> invokeLocalService(methodHandler, dataEncodingMetadata, payload));
                }
                //composite data for return value
                RSocketMimeType resultEncodingType = resultEncodingType(messageAcceptMimeTypesMetadata, dataEncodingMetadata.getRSocketMimeType(), methodHandler);
                Mono<Object> monoResult;
                if (result instanceof Mono) {
                    monoResult = (Mono) result;
                } else {
                    monoResult = methodHandler.getReactiveAdapter().toMono(result);
                }
                return monoResult
                        .map(object -> encodingFacade.encodingResult(object, resultEncodingType))
                        .map(dataByteBuf -> ByteBufPayload.create(dataByteBuf, encodingFacade.getDefaultCompositeMetadataByteBuf(resultEncodingType).retainedDuplicate()));
            } else {
                ReferenceCountUtil.safeRelease(payload);
                return Mono.error(new InvalidException(RsocketErrorCode.message("RST-201404", routing.getService(), routing.getMethod())));
            }
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-200500"), e);
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-900500", e.getMessage())));
        }
    }


    protected Mono<Void> localFireAndForget(GSVRoutingMetadata routing, MessageMimeTypeMetadata dataEncodingMetadata, Payload payload) {
        ReactiveMethodHandler methodHandler = localServiceCaller.getInvokeMethod(routing.getService(), routing.getMethod());
        if (methodHandler != null) {
            if (methodHandler.isAsyncReturn()) {
                try {
                    return methodHandler.getReactiveAdapter().toMono(invokeLocalService(methodHandler, dataEncodingMetadata, payload));
                } catch (Exception e) {
                    ReferenceCountUtil.safeRelease(payload);
                    log.error(RsocketErrorCode.message("RST-200500"), e);
                    return Mono.error(e);
                }
            } else {
                return Mono.create((sink) -> {
                    try {
                        invokeLocalService(methodHandler, dataEncodingMetadata, payload);
                        sink.success();
                    } catch (Exception e) {
                        log.error(RsocketErrorCode.message("RST-200500"), e);
                        sink.error(e);
                    }
                });
            }
        } else {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-201404", routing.getService(), routing.getMethod())));
        }
    }


    protected Flux<Payload> localRequestStream(GSVRoutingMetadata routing,
                                               MessageMimeTypeMetadata dataEncodingMetadata,
                                               @Nullable MessageAcceptMimeTypesMetadata messageAcceptMimeTypesMetadata,
                                               Payload payload) {
        try {
            ReactiveMethodHandler methodHandler = localServiceCaller.getInvokeMethod(routing.getService(), routing.getMethod());
            if (methodHandler != null) {
                Object result = invokeLocalService(methodHandler, dataEncodingMetadata, payload);
                Flux<Object> fluxResult;
                if (result instanceof Flux) {
                    fluxResult = (Flux<Object>) result;
                } else {
                    fluxResult = methodHandler.getReactiveAdapter().toFlux(result);
                }
                //composite data for return value
                RSocketMimeType resultEncodingType = resultEncodingType(messageAcceptMimeTypesMetadata, dataEncodingMetadata.getRSocketMimeType(), methodHandler);
                return fluxResult
                        .map(object -> encodingFacade.encodingResult(object, resultEncodingType))
                        .map(dataByteBuf -> ByteBufPayload.create(dataByteBuf, encodingFacade.getDefaultCompositeMetadataByteBuf(resultEncodingType).retainedDuplicate()));
            } else {
                ReferenceCountUtil.safeRelease(payload);
                return Flux.error(new InvalidException(RsocketErrorCode.message("RST-201404", routing.getService(), routing.getMethod())));
            }
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-200500"), e);
            ReferenceCountUtil.safeRelease(payload);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-900500", e.getMessage())));
        }
    }
    
    @SuppressWarnings("ReactiveStreamsNullableInLambdaInTransform")
    public Flux<Payload> localRequestChannel(GSVRoutingMetadata routing,
                                             MessageMimeTypeMetadata dataEncodingMetadata,
                                             @Nullable MessageAcceptMimeTypesMetadata messageAcceptMimeTypesMetadata,
                                             Payload signal, Flux<Payload> payloads) {
        try {
            ReactiveMethodHandler methodHandler = localServiceCaller.getInvokeMethod(routing.getService(), routing.getMethod());
            if (methodHandler != null) {
                Object result;
                if (methodHandler.getParamCount() == 1) {
                    Flux<Object> paramFlux = payloads
                            .map(payload -> {
                                return encodingFacade.decodeResult(dataEncodingMetadata.getRSocketMimeType(), payload.data(), methodHandler.getInferredClassForParameter(0));
                            });
                    result = methodHandler.invoke(paramFlux);
                } else {
                    Object paramFirst = encodingFacade.decodeResult(dataEncodingMetadata.getRSocketMimeType(), signal.data(), methodHandler.getParameterTypes()[0]);
                    Flux<Object> paramFlux = payloads
                            .map(payload -> {
                                return encodingFacade.decodeResult(dataEncodingMetadata.getRSocketMimeType(), payload.data(), methodHandler.getInferredClassForParameter(1));
                            });
                    result = methodHandler.invoke(paramFirst, paramFlux);
                }
                if (result instanceof Mono) {
                    result = Flux.from((Mono<?>) result);
                }
                //composite data for return value
                RSocketMimeType resultEncodingType = resultEncodingType(messageAcceptMimeTypesMetadata, dataEncodingMetadata.getRSocketMimeType(), methodHandler);
                //result return
                return ((Flux<?>) result)
                        .map(object -> encodingFacade.encodingResult(object, resultEncodingType))
                        .map(dataByteBuf -> ByteBufPayload.create(dataByteBuf, encodingFacade.getDefaultCompositeMetadataByteBuf(resultEncodingType).retainedDuplicate()));
            } else {
                return Flux.error(new InvalidException(RsocketErrorCode.message("RST-201404", routing.getService(), routing.getMethod())));
            }
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-200500"), e);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-900500", e.getMessage())));
        }
    }


    /**
     * invoke local service: nullable for void return
     *
     * @param methodHandler        method handler
     * @param dataEncodingMetadata data encoding metadata
     * @param payload              payload
     * @return result
     * @throws Exception exception
     */
    @Nullable
    protected Object invokeLocalService(ReactiveMethodHandler methodHandler, MessageMimeTypeMetadata dataEncodingMetadata, Payload payload) throws Exception {
        Object result;
        if (methodHandler.getParamCount() > 0) {
            Object args = encodingFacade.decodeParams(dataEncodingMetadata.getRSocketMimeType(), payload.data(), methodHandler.getParameterTypes());
            if (args instanceof Object[]) {
                result = methodHandler.invoke((Object[]) args);
            } else {
                result = methodHandler.invoke(args);
            }
        } else {
            result = methodHandler.invoke();
        }
        return result;
    }

    private RSocketMimeType resultEncodingType(@Nullable MessageAcceptMimeTypesMetadata messageAcceptMimeTypesMetadata,
                                               RSocketMimeType defaultEncodingType,
                                               ReactiveMethodHandler reactiveMethodHandler) {
        if (reactiveMethodHandler.isBinaryReturn()) {
            return RSocketMimeType.Binary;
        }
        if (messageAcceptMimeTypesMetadata != null) {
            RSocketMimeType firstAcceptType = messageAcceptMimeTypesMetadata.getFirstAcceptType();
            if (firstAcceptType != null) {
                return firstAcceptType;
            }
        }
        return defaultEncodingType;
    }
}
