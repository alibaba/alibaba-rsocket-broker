package com.alibaba.rsocket.listen;

import com.alibaba.rsocket.encoding.RSocketEncodingFacade;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.MessageTagsMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.rpc.ReactiveMethodHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cloudevents.v1.CloudEventImpl;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.exceptions.InvalidException;
import io.rsocket.util.DefaultPayload;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * rsocket responder support for both sides, supply some base api
 *
 * @author leijuan
 */
public abstract class RSocketResponderSupport extends AbstractRSocket implements AttributeMap {
    public static String COMPOSITE_METADATA_KEY = CompositeMetadataRSocket.COMPOSITE_METADATA_KEY;
    protected Logger log = LoggerFactory.getLogger(this.getClass());
    protected LocalReactiveServiceCaller localServiceCaller;
    protected RSocketEncodingFacade encodingFacade = RSocketEncodingFacade.getInstance();
    public static TypeReference<CloudEventImpl<ObjectNode>> CLOUD_EVENT_TYPE_REFERENCE = new TypeReference<CloudEventImpl<ObjectNode>>() {
    };
    /**
     * attribute map
     */
    private Map<String, Object> attributes = new ConcurrentHashMap<>();

    protected Mono<Payload> localRequestResponse(GSVRoutingMetadata routing, MessageMimeTypeMetadata dataEncodingMetadata, Payload payload) {
        try {
            ReactiveMethodHandler methodHandler = localServiceCaller.getInvokeMethod(routing.getService(), routing.getMethod());
            if (methodHandler != null) {
                Object result;
                if (methodHandler.isAsyncReturn()) {
                    result = Mono.fromCallable(() -> invokeLocalService(methodHandler, dataEncodingMetadata, routing, payload));
                } else {
                    result = invokeLocalService(methodHandler, dataEncodingMetadata, routing, payload);
                }
                //composite data for return value
                RSocketCompositeMetadata resultCompositeMetadata = RSocketCompositeMetadata.from(dataEncodingMetadata);
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
                } else if (result instanceof Maybe) {
                    monoResult = RxJava2Adapter.maybeToMono((Maybe) result);
                } else if (result instanceof Single) {
                    monoResult = RxJava2Adapter.singleToMono((Single) result);
                } else {  //CompletableFuture
                    monoResult = Mono.fromFuture((CompletableFuture) result);
                }
                return monoResult
                        .map(object -> encodingFacade.encodingResult(object, dataEncodingMetadata.getRSocketMimeType()))
                        .map(dataByteBuf -> DefaultPayload.create(dataByteBuf, resultCompositeMetadata.getContent()))
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
            return Mono.fromRunnable(() -> {
                try {
                    invokeLocalService(methodHandler, dataEncodingMetadata, routing, payload);
                } catch (Exception e) {
                    log.error(RsocketErrorCode.message("RST-200500"), e);
                } finally {
                    ReferenceCountUtil.safeRelease(payload);
                }
            });
        } else {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-201404", routing.getService(), routing.getMethod())));
        }
    }


    protected Flux<Payload> localRequestStream(GSVRoutingMetadata routing, MessageMimeTypeMetadata dataEncodingMetadata, Payload payload) {
        try {
            ReactiveMethodHandler methodHandler = localServiceCaller.getInvokeMethod(routing.getService(), routing.getMethod());
            if (methodHandler != null) {
                Object result = invokeLocalService(methodHandler, dataEncodingMetadata, routing, payload);
                ReferenceCountUtil.safeRelease(payload);
                Flux<Object> fluxResult;
                if (result instanceof Iterable) {
                    fluxResult = Flux.fromIterable((Iterable) result);
                } else if (result instanceof Flux) {
                    fluxResult = (Flux) result;
                } else {
                    fluxResult = Flux.just(result);
                }
                //composite data for return value
                RSocketCompositeMetadata resultCompositeMetadata = RSocketCompositeMetadata.from(dataEncodingMetadata);
                return fluxResult
                        .map(object -> encodingFacade.encodingResult(object, dataEncodingMetadata.getRSocketMimeType()))
                        .map(dataByteBuf -> DefaultPayload.create(dataByteBuf, resultCompositeMetadata.getContent()));
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

    public Flux<Payload> localRequestChannel(GSVRoutingMetadata routing, MessageMimeTypeMetadata dataEncodingMetadata, Payload signal, Flux<Payload> payloads) {
        try {
            ReactiveMethodHandler methodHandler = localServiceCaller.getInvokeMethod(routing.getService(), routing.getMethod());
            if (methodHandler != null) {
                Object result;
                if (methodHandler.getParameterCount() == 1) {
                    Flux<Object> paramFlux = payloads
                            .map(payload -> {
                                return encodingFacade.decodeResult(dataEncodingMetadata.getRSocketMimeType(), payload.data(), methodHandler.getInferredClassForParameter(0));
                            });
                    result = localServiceCaller.invoke(routing.getService(), routing.getMethod(), paramFlux);
                } else {
                    Object paramFirst = encodingFacade.decodeResult(dataEncodingMetadata.getRSocketMimeType(), signal.data(), methodHandler.getParameterTypes()[0]);
                    Flux<Object> paramFlux = payloads
                            .map(payload -> {
                                return encodingFacade.decodeResult(dataEncodingMetadata.getRSocketMimeType(), payload.data(), methodHandler.getInferredClassForParameter(1));
                            });
                    result = localServiceCaller.invoke(routing.getService(), routing.getMethod(), paramFirst, paramFlux);
                }
                //composite data for return value
                RSocketCompositeMetadata resultCompositeMetadata = RSocketCompositeMetadata.from(dataEncodingMetadata);
                //result return
                return ((Flux<Object>) result)
                        .map(object -> encodingFacade.encodingResult(object, dataEncodingMetadata.getRSocketMimeType()))
                        .map(dataByteBuf -> DefaultPayload.create(dataByteBuf, resultCompositeMetadata.getContent()));
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
     * @param routingMetadata      routing metadata
     * @param payload              payload
     * @return result
     * @throws Exception exception
     */
    @Nullable
    protected Object invokeLocalService(ReactiveMethodHandler methodHandler, MessageMimeTypeMetadata dataEncodingMetadata,
                                        GSVRoutingMetadata routingMetadata, Payload payload) throws Exception {
        Object result;
        if (methodHandler.getParameterCount() > 0) {
            Object args = encodingFacade.decodeParams(dataEncodingMetadata.getRSocketMimeType(), payload.data(), methodHandler.getParameterTypes());
            if (args instanceof Object[]) {
                result = localServiceCaller.invoke(routingMetadata.getService(), routingMetadata.getMethod(), (Object[]) args);
            } else {
                result = localServiceCaller.invoke(routingMetadata.getService(), routingMetadata.getMethod(), args);
            }
        } else {
            result = localServiceCaller.invoke(routingMetadata.getService(), routingMetadata.getMethod());
        }
        return result;
    }

    @Override
    public @Nullable Object attr(String name) {
        return attributes.get(name);
    }

    @Override
    public void attr(String name, @Nullable Object value) {
        attributes.put(name, value);
    }

}
