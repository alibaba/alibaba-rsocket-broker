package com.alibaba.rsocket.invocation;


import com.alibaba.rsocket.MutableContext;
import com.alibaba.rsocket.encoding.RSocketEncodingFacade;
import com.alibaba.rsocket.metadata.*;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Metrics;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.rsocket.Payload;
import io.rsocket.util.DefaultPayload;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.cache.annotation.CacheResult;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * RSocket requester RPC proxy for remote service
 *
 * @author leijuan
 */
public class RSocketRequesterRpcProxy implements InvocationHandler {
    private UpstreamCluster upstream;
    /**
     * service interface
     */
    private Class<?> serviceInterface;
    /**
     * cached methods
     */
    private Map<Method, CacheResult> cachedMethods = new HashMap<>();
    /**
     * group, such as datacenter name, region name
     */
    private String group;
    /**
     * service name
     */
    private String service;
    /**
     * service version
     */
    private String version;
    /**
     * encoding type
     */
    private RSocketMimeType encodingType;
    /**
     * '
     * timeout for request/response
     */
    private Duration timeout;
    /**
     * encoding facade
     */
    private RSocketEncodingFacade encodingFacade = RSocketEncodingFacade.getInstance();

    public static Cache<String, Mono<Object>> rpcCache = Caffeine.newBuilder()
            .maximumSize(500_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    /**
     * java method metadata map cache for performance
     */
    private Map<Method, ReactiveMethodMetadata> methodMetadataMap = new ConcurrentHashMap<>();

    public RSocketRequesterRpcProxy(UpstreamCluster upstream,
                                    String group, Class<?> serviceInterface, String service, String version,
                                    RSocketMimeType encodingType, Duration timeout) {
        this.upstream = upstream;
        this.serviceInterface = serviceInterface;
        this.service = serviceInterface.getCanonicalName();
        if (service != null && !service.isEmpty()) {
            this.service = service;
        }
        this.group = group;
        this.version = version;
        this.encodingType = encodingType;
        this.timeout = timeout;
        for (Method method : serviceInterface.getMethods()) {
            if (method.getAnnotation(CacheResult.class) != null) {
                cachedMethods.put(method, method.getAnnotation(CacheResult.class));
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MutableContext mutableContext = new MutableContext();
        //performance, cache method
        if (!methodMetadataMap.containsKey(method)) {
            methodMetadataMap.put(method, new ReactiveMethodMetadata(method, encodingType));
        }
        ReactiveMethodMetadata methodMetadata = methodMetadataMap.get(method);
        //payload metadata
        GSVRoutingMetadata routing = new GSVRoutingMetadata();
        routing.setService(this.service);
        routing.setMethod(methodMetadata.getName());
        if (group != null) {
            routing.setGroup(this.group);
        }
        if (version != null) {
            routing.setVersion(this.version);
        }
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(routing);
        //add param encoding & make hessian as result type
        if (methodMetadata.getParamEncoding() != null) {
            compositeMetadata.addMetadata(new DataEncodingMetadata(methodMetadata.getParamEncoding(), encodingType));
        }
        //metadata data content
        ByteBuf compositeMetadataBuf = compositeMetadata.getContent();
        //----- return type deal------
        if (methodMetadata.isBiDirectional()) { //bi directional, channel
            metrics(routing, "0x07");
            Payload routePayload;
            Flux<Object> source;
            //1 param or 2 params
            if (args.length == 1) {
                routePayload = DefaultPayload.create(Unpooled.EMPTY_BUFFER, compositeMetadataBuf);
                source = (Flux<Object>) args[0];
            } else {
                ByteBuf bodyBuffer = encodingFacade.encodingResult(args[0], methodMetadata.getParamEncoding());
                routePayload = DefaultPayload.create(bodyBuffer, compositeMetadataBuf);
                source = (Flux<Object>) args[1];
            }
            Flux<Payload> payloadFlux = Flux.just(routePayload).mergeWith(source.map(obj -> DefaultPayload.create(encodingFacade.encodingResult(obj, encodingType), compositeMetadataBuf)));
            Flux<Payload> payloads = upstream.requestChannel(payloadFlux);
            return payloads.flatMap(payload -> {
                try {
                    return Mono.justOrEmpty(encodingFacade.decodeResult(encodingType, payload.data(), methodMetadata.getInferredClassForResult()));
                } catch (Exception e) {
                    return Flux.error(e);
                } finally {
                    // ReferenceCountUtil.safeRelease(payload);
                }
            }).subscriberContext(mutableContext::putAll);
        } else {
            //body content
            ByteBuf bodyBuffer = encodingFacade.encodingParams(args, methodMetadata.getParamEncoding());
            //return type is void, use fireAndForget
            Class<?> returnType = method.getReturnType();
            if (returnType.equals(Void.TYPE)) {
                metrics(routing, "0x05");
                upstream.fireAndForget(DefaultPayload.create(bodyBuffer, compositeMetadataBuf)).subscribe();
                return null;
            }
            //request/stream
            else if (returnType.equals(Flux.class) || returnType.equals(Flowable.class)) {
                metrics(routing, "0x05");
                Flux<Payload> flux = upstream.requestStream(DefaultPayload.create(bodyBuffer, compositeMetadataBuf));
                Flux<Object> result = flux.flatMap(payload -> {
                    try {
                        return Mono.justOrEmpty(encodingFacade.decodeResult(encodingType, payload.data(), methodMetadata.getInferredClassForResult()));
                    } catch (Exception e) {
                        return Mono.error(e);
                    } finally {
                        ReferenceCountUtil.safeRelease(payload);
                    }
                });
                if (returnType.equals(Flowable.class)) {
                    return RxJava2Adapter.fluxToFlowable(result);
                }
                return result.subscriberContext(mutableContext::putAll);
            }
            // request/response
            else {
                metrics(routing, "0x04");
                if (cachedMethods.containsKey(method)) {
                    CacheResult cacheResult = cachedMethods.get(method);
                    String key = cacheResult.cacheName() + ":" + generateCacheKey(args);
                    Mono<Object> cachedMono = rpcCache.getIfPresent(key);
                    if (cachedMono != null) {
                        return cachedMono;
                    }
                }
                Mono<Payload> payloadMono = upstream.requestResponse(DefaultPayload.create(bodyBuffer, compositeMetadataBuf)).timeout(timeout);
                Mono<Object> result = payloadMono.flatMap(payload -> {
                    try {
                        Mono<Object> remoteResult = Mono.justOrEmpty(encodingFacade.decodeResult(encodingType, payload.data(), methodMetadata.getInferredClassForResult()));
                        return injectContext(payload, remoteResult);
                    } catch (Exception e) {
                        return Mono.error(e);
                    } finally {
                        ReferenceCountUtil.safeRelease(payload);
                    }
                }).doOnNext(obj -> {
                    if (cachedMethods.containsKey(method)) {
                        CacheResult cacheResult = cachedMethods.get(method);
                        String key = cacheResult.cacheName() + ":" + generateCacheKey(args);
                        rpcCache.put(key, Mono.just(obj).cache());
                    }
                });
                if (returnType.equals(Maybe.class)) {
                    return RxJava2Adapter.monoToMaybe(result);
                } else if (returnType.equals(Single.class)) {
                    return RxJava2Adapter.monoToSingle(result);
                }
                return result.subscriberContext(mutableContext::putAll);
            }
        }
    }

    @SuppressWarnings("Duplicates")
    protected void metrics(GSVRoutingMetadata routing, String frameType) {
        List<String> tags = new ArrayList<>();
        if (routing.getGroup() != null && !routing.getGroup().isEmpty()) {
            tags.add("group");
            tags.add(routing.getGroup());
        }
        if (routing.getVersion() != null && !routing.getVersion().isEmpty()) {
            tags.add("version");
            tags.add(routing.getVersion());
        }
        tags.add("method");
        tags.add(routing.getMethod());
        tags.add("frame");
        tags.add(frameType);
        Metrics.counter(routing.getService(), tags.toArray(new String[0])).increment();
    }

    /**
     * Invalidate RPC cache
     *
     * @param key cache key
     */
    public static void invalidateCache(String key) {
        rpcCache.invalidate(key);
    }

    /**
     * Generate cache key, compatible with Spring SimpleKeyGenerator
     *
     * @param params params
     * @return hashcode
     */
    public static Integer generateCacheKey(Object... params) {
        if (params == null || params.length == 0) {
            return 0;
        } else if (params.length == 1) {
            Object param = params[0];
            if (param != null && !param.getClass().isArray()) {
                return param.hashCode();
            }
        }
        return Arrays.deepHashCode(params);
    }

    Mono<Object> injectContext(Payload payload, Mono<Object> origin) throws Exception {
        Mono<Object> temp = origin;
        ByteBuf metadata = payload.metadata();
        if (metadata.capacity() > 0) {
            RSocketCompositeMetadata rSocketCompositeMetadata = RSocketCompositeMetadata.from(metadata);
            //message tags
            if (rSocketCompositeMetadata.contains(RSocketMimeType.MessageTags)) {
                MessageTagsMetadata tagsMetadata = MessageTagsMetadata.from(rSocketCompositeMetadata.getMetadata(RSocketMimeType.MessageTags));
                Map<String, String> tags = tagsMetadata.getTags();
                temp = temp.subscriberContext(ctx -> ctx.put("_tags", tags));
            }
        }
        return temp;
    }
}
