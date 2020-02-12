package com.alibaba.rsocket.listen;

import com.alibaba.rsocket.encoding.RSocketEncodingFacade;
import com.alibaba.rsocket.metadata.*;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.rpc.ReactiveMethodHandler;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.exceptions.InvalidException;
import io.rsocket.util.ByteBufPayload;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

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
                RSocketMimeType resultEncodingType = resultEncodingType(messageAcceptMimeTypesMetadata, dataEncodingMetadata.getRSocketMimeType());
                RSocketCompositeMetadata resultCompositeMetadata = RSocketCompositeMetadata.from(new MessageMimeTypeMetadata(resultEncodingType));
                Mono<Object> monoResult;
                if (result instanceof Mono) {
                    monoResult = (Mono) result;
                    //Mono name & tags support
                    Scannable scannable = Scannable.from(monoResult);
                    if (scannable.isScanAvailable()) {
                        Stream<Tuple2<String, String>> tagsStream = scannable.scan(Scannable.Attr.TAGS);
                        if (tagsStream != null) {
                            Map<String, String> tags = new HashMap<>();
                            tagsStream.forEach((tuple) -> {
                                tags.put(tuple.getT1(), tuple.getT2());
                            });
                            resultCompositeMetadata.addMetadata(new MessageTagsMetadata(tags));
                        }
                    }
                } else {
                    monoResult = methodHandler.getReactiveAdapter().toMono(result);
                }
                ByteBuf compositeMetadataContent = resultCompositeMetadata.getContent();
                return monoResult
                        .map(object -> encodingFacade.encodingResult(object, resultEncodingType))
                        .map(dataByteBuf -> ByteBufPayload.create(dataByteBuf, compositeMetadataContent))
                        .doOnTerminate(() -> {
                            ReferenceCountUtil.safeRelease(payload);
                        });
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
                    } finally {
                        ReferenceCountUtil.safeRelease(payload);
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
                RSocketMimeType resultEncodingType = resultEncodingType(messageAcceptMimeTypesMetadata, dataEncodingMetadata.getRSocketMimeType());
                RSocketCompositeMetadata resultCompositeMetadata = RSocketCompositeMetadata.from(new MessageMimeTypeMetadata(resultEncodingType));
                ByteBuf compositeMetadataContent = resultCompositeMetadata.getContent();
                return fluxResult
                        .map(object -> encodingFacade.encodingResult(object, resultEncodingType))
                        .map(dataByteBuf -> ByteBufPayload.create(dataByteBuf, compositeMetadataContent))
                        .doOnTerminate(() -> {
                            ReferenceCountUtil.safeRelease(payload);
                        });
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

    @Override
    public final Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return Flux.error(new InvalidException(RsocketErrorCode.message("RST-201400")));
    }

    public Flux<Payload> localRequestChannel(GSVRoutingMetadata routing,
                                             MessageMimeTypeMetadata dataEncodingMetadata,
                                             @Nullable MessageAcceptMimeTypesMetadata messageAcceptMimeTypesMetadata,
                                             Payload signal, Flux<Payload> payloads) {
        try {
            ReactiveMethodHandler methodHandler = localServiceCaller.getInvokeMethod(routing.getService(), routing.getMethod());
            if (methodHandler != null) {
                Object result;
                if (methodHandler.getParameterCount() == 1) {
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
                //composite data for return value
                RSocketMimeType resultEncodingType = resultEncodingType(messageAcceptMimeTypesMetadata, dataEncodingMetadata.getRSocketMimeType());
                RSocketCompositeMetadata resultCompositeMetadata = RSocketCompositeMetadata.from(dataEncodingMetadata);
                ByteBuf compositeMetadataContent = resultCompositeMetadata.getContent();
                //result return
                return ((Flux<Object>) result)
                        .map(object -> encodingFacade.encodingResult(object, resultEncodingType))
                        .map(dataByteBuf -> ByteBufPayload.create(dataByteBuf, compositeMetadataContent))
                        .doOnTerminate(() -> {
                            ReferenceCountUtil.safeRelease(compositeMetadataContent);
                        });
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
        if (methodHandler.getParameterCount() > 0) {
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

    private RSocketMimeType resultEncodingType(@Nullable MessageAcceptMimeTypesMetadata messageAcceptMimeTypesMetadata, RSocketMimeType defaultEncodingType) {
        RSocketMimeType encodingType = defaultEncodingType;
        if (messageAcceptMimeTypesMetadata != null) {
            RSocketMimeType firstAcceptType = messageAcceptMimeTypesMetadata.getFirstAcceptType();
            if (firstAcceptType != null) {
                encodingType = firstAcceptType;
            }
        }
        return encodingType;
    }
}
