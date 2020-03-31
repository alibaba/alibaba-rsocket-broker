package com.alibaba.rsocket.invocation;

import com.alibaba.rsocket.MutableContext;
import com.alibaba.rsocket.encoding.RSocketEncodingFacade;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import io.micrometer.core.instrument.Metrics;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.frame.FrameType;
import io.rsocket.util.ByteBufPayload;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;


/**
 * RSocket requester RPC proxy for remote service
 *
 * @author leijuan
 */
public class RSocketRequesterRpcProxy implements InvocationHandler {
    private static Logger log = LoggerFactory.getLogger(RSocketRequesterRpcProxy.class);
    protected RSocket rsocket;
    /**
     * service interface
     */
    protected Class<?> serviceInterface;
    /**
     * group, such as data center name, region name or virtual cluster name
     */
    protected String group;
    /**
     * service name
     */
    protected String service;
    /**
     * service version
     */
    protected String version;
    /**
     * endpoint of service
     */
    protected String endpoint;
    /**
     * encoding type
     */
    protected RSocketMimeType encodingType;
    /**
     * accept encoding types
     */
    protected RSocketMimeType[] acceptEncodingTypes;
    /**
     * '
     * timeout for request/response
     */
    protected Duration timeout;
    /**
     * encoding facade
     */
    protected RSocketEncodingFacade encodingFacade = RSocketEncodingFacade.getInstance();
    /**
     * java method metadata map cache for performance
     */
    protected Map<Method, ReactiveMethodMetadata> methodMetadataMap = new ConcurrentHashMap<>();
    /**
     * interface default method handlers
     */
    protected Map<Method, MethodHandle> defaultMethodHandles = new HashMap<>();

    public RSocketRequesterRpcProxy(UpstreamCluster upstream,
                                    String group, Class<?> serviceInterface, @Nullable String service, String version,
                                    RSocketMimeType encodingType, @Nullable RSocketMimeType acceptEncodingType,
                                    Duration timeout, @Nullable String endpoint) {
        this.rsocket = upstream.getLoadBalancedRSocket();
        this.serviceInterface = serviceInterface;
        this.service = serviceInterface.getCanonicalName();
        if (service != null && !service.isEmpty()) {
            this.service = service;
        }
        this.group = group;
        this.version = version;
        this.endpoint = endpoint;
        this.encodingType = encodingType;
        if (acceptEncodingType == null) {
            this.acceptEncodingTypes = defaultAcceptEncodingTypes();
        } else {
            this.acceptEncodingTypes = new RSocketMimeType[]{acceptEncodingType};
        }
        this.timeout = timeout;
    }

    @Override
    @RuntimeType
    public Object invoke(@This Object proxy, @Origin Method method, @AllArguments Object[] args) throws Throwable {
        //interface default method validation for JDK Proxy only, not necessary for ByteBuddy
        /*if (method.isDefault()) {
            return getMethodHandle(method, serviceInterface).bindTo(proxy).invokeWithArguments(args);
        }*/
        MutableContext mutableContext = new MutableContext();
        if (!methodMetadataMap.containsKey(method)) {
            methodMetadataMap.put(method, new ReactiveMethodMetadata(group, service, version,
                    method, encodingType, this.acceptEncodingTypes, endpoint));
        }
        ReactiveMethodMetadata methodMetadata = methodMetadataMap.get(method);
        //----- return type deal------
        if (methodMetadata.getRsocketFrameType() == FrameType.REQUEST_CHANNEL) {
            metrics(methodMetadata);
            Payload routePayload;
            Flux<Object> source;
            //1 param or 2 params
            if (args.length == 1) {
                routePayload = ByteBufPayload.create(Unpooled.EMPTY_BUFFER, methodMetadata.getCompositeMetadataByteBuf().retainedDuplicate());
                source = (Flux<Object>) args[0];
            } else {
                ByteBuf bodyBuffer = encodingFacade.encodingResult(args[0], methodMetadata.getParamEncoding());
                routePayload = ByteBufPayload.create(bodyBuffer, methodMetadata.getCompositeMetadataByteBuf().retainedDuplicate());
                source = (Flux<Object>) args[1];
            }
            Flux<Payload> payloadFlux = source.startWith(routePayload).map(obj -> {
                if (obj instanceof Payload) return (Payload) obj;
                return ByteBufPayload.create(encodingFacade.encodingResult(obj, encodingType),
                        methodMetadata.getCompositeMetadataByteBuf().retainedDuplicate());
            });
            Flux<Payload> payloads = rsocket.requestChannel(payloadFlux);
            Flux<Object> fluxReturn = payloads.concatMap(payload -> {
                try {
                    RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
                    return Mono.justOrEmpty(encodingFacade.decodeResult(extractPayloadDataMimeType(compositeMetadata, encodingType), payload.data(), methodMetadata.getInferredClassForReturn()));
                } catch (Exception e) {
                    return Flux.error(e);
                }
            }).subscriberContext(mutableContext::putAll);
            if (methodMetadata.isMonoChannel()) {
                return fluxReturn.last();
            } else {
                return fluxReturn;
            }
        } else {
            //body content
            ByteBuf bodyBuffer = encodingFacade.encodingParams(args, methodMetadata.getParamEncoding());
            Class<?> returnType = method.getReturnType();
            if (methodMetadata.getRsocketFrameType() == FrameType.REQUEST_RESPONSE) {
                metrics(methodMetadata);
                Mono<Payload> payloadMono = remoteRequestResponse(methodMetadata, methodMetadata.getCompositeMetadataByteBuf().retainedDuplicate(), bodyBuffer);
                Mono<Object> result = payloadMono.handle((payload, sink) -> {
                    try {
                        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
                        Object obj = encodingFacade.decodeResult(extractPayloadDataMimeType(compositeMetadata, encodingType), payload.data(), methodMetadata.getInferredClassForReturn());
                        if (obj != null) {
                            sink.next(obj);
                        }
                        sink.complete();
                    } catch (Exception e) {
                        sink.error(e);
                    }
                });
                return methodMetadata.getReactiveAdapter().fromPublisher(result, returnType, mutableContext);
            } else if (methodMetadata.getRsocketFrameType() == FrameType.REQUEST_FNF) {
                metrics(methodMetadata);
                return remoteFireAndForget(methodMetadata, methodMetadata.getCompositeMetadataByteBuf().retainedDuplicate(), bodyBuffer);
            } else if (methodMetadata.getRsocketFrameType() == FrameType.REQUEST_STREAM) {
                metrics(methodMetadata);
                Flux<Payload> flux = remoteRequestStream(methodMetadata, methodMetadata.getCompositeMetadataByteBuf().retainedDuplicate(), bodyBuffer);
                Flux<Object> result = flux.concatMap((payload) -> {
                    try {
                        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
                        return Mono.justOrEmpty(encodingFacade.decodeResult(extractPayloadDataMimeType(compositeMetadata, encodingType), payload.data(), methodMetadata.getInferredClassForReturn()));
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
                return methodMetadata.getReactiveAdapter().fromPublisher(result, returnType, mutableContext);
            } else {
                ReferenceCountUtil.safeRelease(bodyBuffer);
                return Mono.error(new Exception(RsocketErrorCode.message("RST-200405", methodMetadata.getRsocketFrameType())));
            }
        }
    }

    protected Flux<Payload> remoteRequestStream(ReactiveMethodMetadata methodMetadata, ByteBuf compositeMetadata, ByteBuf bodyBuf) {
        return rsocket.requestStream(ByteBufPayload.create(bodyBuf, compositeMetadata));
    }

    protected Mono<Void> remoteFireAndForget(ReactiveMethodMetadata methodMetadata, ByteBuf compositeMetadata, ByteBuf bodyBuf) {
        return rsocket.fireAndForget(ByteBufPayload.create(bodyBuf, compositeMetadata));
    }

    @NotNull
    protected Mono<Payload> remoteRequestResponse(ReactiveMethodMetadata methodMetadata, ByteBuf compositeMetadata, ByteBuf bodyBuf) {
        return rsocket.requestResponse(ByteBufPayload.create(bodyBuf, compositeMetadata))
                .name(methodMetadata.getFullName())
                .metrics()
                .timeout(timeout)
                .doOnError(TimeoutException.class, e -> {
                    timeOutMetrics(methodMetadata);
                    log.error(RsocketErrorCode.message("RST-200503", methodMetadata.getFullName(), timeout));
                });
    }

    protected void metrics(ReactiveMethodMetadata methodMetadata) {
        Metrics.counter(this.service, methodMetadata.getMetricsTags());
    }

    protected void timeOutMetrics(ReactiveMethodMetadata methodMetadata) {
        Metrics.counter("rsocket.timeout.error", methodMetadata.getMetricsTags());
    }

    private RSocketMimeType extractPayloadDataMimeType(RSocketCompositeMetadata compositeMetadata, RSocketMimeType defaultEncodingType) {
        if (compositeMetadata.contains(RSocketMimeType.MessageMimeType)) {
            MessageMimeTypeMetadata mimeTypeMetadata = MessageMimeTypeMetadata.from(compositeMetadata.getMetadata(RSocketMimeType.MessageMimeType));
            return mimeTypeMetadata.getRSocketMimeType();
        }
        return defaultEncodingType;
    }

    @Deprecated
    public MethodHandle getMethodHandle(Method method, Class<?> serviceInterface) throws Exception {
        MethodHandle methodHandle = this.defaultMethodHandles.get(method);
        if (methodHandle == null) {
            String version = System.getProperty("java.version");
            if (version.startsWith("1.8.")) {
                Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Integer.TYPE);
                if (!lookupConstructor.isAccessible()) {
                    lookupConstructor.setAccessible(true);
                }
                methodHandle = lookupConstructor.newInstance(method.getDeclaringClass(), MethodHandles.Lookup.PRIVATE)
                        .unreflectSpecial(method, method.getDeclaringClass());
            } else {
                methodHandle = MethodHandles.lookup().findSpecial(
                        method.getDeclaringClass(),
                        method.getName(),
                        MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                        serviceInterface);
            }
            this.defaultMethodHandles.put(method, methodHandle);
        }
        return methodHandle;
    }

    public RSocketMimeType[] defaultAcceptEncodingTypes() {
        return new RSocketMimeType[]{RSocketMimeType.Hessian, RSocketMimeType.Java_Object,
                RSocketMimeType.Json, RSocketMimeType.Protobuf, RSocketMimeType.Avor, RSocketMimeType.CBOR,
                RSocketMimeType.Text, RSocketMimeType.Binary};
    }
}
