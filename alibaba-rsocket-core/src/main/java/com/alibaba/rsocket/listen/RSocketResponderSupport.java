package com.alibaba.rsocket.listen;

import com.alibaba.rsocket.AbstractRSocket;
import com.alibaba.rsocket.encoding.RSocketEncodingFacade;
import com.alibaba.rsocket.encoding.impl.RSocketEncodingFacadeImpl;
import com.alibaba.rsocket.metadata.*;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.rpc.ReactiveMethodHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Payload;
import io.rsocket.exceptions.InvalidException;
import io.rsocket.util.ByteBufPayload;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * rsocket responder support for both sides, supply some base api
 *
 * @author leijuan
 */
public abstract class RSocketResponderSupport extends AbstractRSocket {
    protected Logger log = LoggerFactory.getLogger(this.getClass());
    protected LocalReactiveServiceCaller localServiceCaller;
    /**
     * responder for sourcing, such as upstream, downstream from listener
     * data format as upstream/downstream + app_name + service names, such as upstream:broker:*, downstream:app-name:*
     */
    protected String sourcing;
    public static final RSocketEncodingFacade encodingFacade = RSocketEncodingFacade.getInstance(new RSocketEncodingFacadeImpl());
    private Map<String, ByteBuf> compositeMetadataForMimeTypes = new HashMap<>();

    public void setSourcing(String sourcing) {
        this.sourcing = sourcing;
    }

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
                    result = Mono.create((sink) -> {
                        try {
                            Object resultObj = invokeLocalService(methodHandler, dataEncodingMetadata, payload);
                            if (resultObj == null) {
                                sink.success();
                            } else if (resultObj instanceof Mono) {
                                Mono<Object> monoObj = (Mono<Object>) resultObj;
                                monoObj.doOnError(sink::error)
                                        .doOnNext(sink::success)
                                        .thenEmpty(Mono.fromRunnable(sink::success))
                                        .subscribe();
                            } else {
                                sink.success(resultObj);
                            }
                        } catch (Exception e) {
                            sink.error(e);
                        }
                    });
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
                        .map(dataByteBuf -> ByteBufPayload.create(dataByteBuf, getCompositeMetadataWithEncoding(resultEncodingType.getType())));
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
                        .map(dataByteBuf -> ByteBufPayload.create(dataByteBuf, getCompositeMetadataWithEncoding(resultEncodingType.getType())));
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
                    Object lastParam = methodHandler.getReactiveAdapter().fromPublisher(paramFlux, methodHandler.getLastParamType());
                    result = methodHandler.invoke(lastParam);
                } else {
                    Object paramFirst = encodingFacade.decodeResult(dataEncodingMetadata.getRSocketMimeType(), signal.data(), methodHandler.getParameterTypes()[0]);
                    Flux<Object> paramFlux = payloads
                            .map(payload -> {
                                return encodingFacade.decodeResult(dataEncodingMetadata.getRSocketMimeType(), payload.data(), methodHandler.getInferredClassForParameter(1));
                            });
                    Object lastParam = methodHandler.getReactiveAdapter().fromPublisher(paramFlux, methodHandler.getLastParamType());
                    result = methodHandler.invoke(paramFirst, lastParam);
                }
                if (result instanceof Mono) {
                    result = Flux.from((Mono<?>) result);
                } else {
                    result = methodHandler.getReactiveAdapter().toFlux(result);
                }
                //composite data for return value
                RSocketMimeType resultEncodingType = resultEncodingType(messageAcceptMimeTypesMetadata, dataEncodingMetadata.getRSocketMimeType(), methodHandler);
                //result return
                return ((Flux<?>) result)
                        .map(object -> encodingFacade.encodingResult(object, resultEncodingType))
                        .map(dataByteBuf -> ByteBufPayload.create(dataByteBuf, getCompositeMetadataWithEncoding(resultEncodingType.getType())));
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

    protected ByteBuf getCompositeMetadataWithEncoding(String mimeType) {
        if (!this.compositeMetadataForMimeTypes.containsKey(mimeType)) {
            RSocketCompositeMetadata resultCompositeMetadata = RSocketCompositeMetadata.from(new MessageMimeTypeMetadata(mimeType));
            ByteBuf compositeMetadataContent = resultCompositeMetadata.getContent();
            this.compositeMetadataForMimeTypes.put(mimeType, Unpooled.copiedBuffer(compositeMetadataContent));
            ReferenceCountUtil.safeRelease(compositeMetadataContent);
        }
        return compositeMetadataForMimeTypes.get(mimeType).retainedDuplicate();
    }
}
