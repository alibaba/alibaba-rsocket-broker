package com.alibaba.rsocket.gateway.grpc;

import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.google.protobuf.GeneratedMessageV3;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC reactive call interceptor with RSocket as backend
 *
 * @author leijuan
 */
public class GrpcReactiveCallInterceptor implements RSocketGrpcSupport {
    private String group;
    private String service;
    private String version;
    private Duration timeout = Duration.ofMillis(3000);
    private RSocket rsocket;
    private final Map<Method, ReactiveGrpcMethodMetadata> methodMetadataMap = new ConcurrentHashMap<>();

    public GrpcReactiveCallInterceptor() {
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public RSocket getRsocket() {
        return rsocket;
    }

    public void setRsocket(RSocket rsocket) {
        this.rsocket = rsocket;
    }

    @SuppressWarnings("unchecked")
    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] params) {
        if (!methodMetadataMap.containsKey(method)) {
            methodMetadataMap.put(method, new ReactiveGrpcMethodMetadata(method, group, service, version));
        }
        ReactiveGrpcMethodMetadata methodMetadata = methodMetadataMap.get(method);
        if (methodMetadata.getRpcType().equals(ReactiveGrpcMethodMetadata.UNARY)) {
            Mono<GeneratedMessageV3> monoParam = (Mono<GeneratedMessageV3>) params[0];
            return monoParam
                    .map(param -> ByteBufPayload.create(Unpooled.wrappedBuffer(param.toByteArray()), methodMetadata.getCompositeMetadataByteBuf().retainedDuplicate()))
                    .flatMap(requestPayload -> rsocketRpc(rsocket, requestPayload, methodMetadata.getInferredClassForReturn()).timeout(this.timeout));
        } else if (methodMetadata.getRpcType().equals(ReactiveGrpcMethodMetadata.SERVER_STREAMING)) {
            Mono<GeneratedMessageV3> monoParam = (Mono<GeneratedMessageV3>) params[0];
            return monoParam
                    .map(param -> ByteBufPayload.create(Unpooled.wrappedBuffer(param.toByteArray()), methodMetadata.getCompositeMetadataByteBuf().retainedDuplicate()))
                    .flatMapMany(requestPayload -> rsocketStream(rsocket, requestPayload, methodMetadata.getInferredClassForReturn())).timeout(this.timeout);
        } else if (methodMetadata.getRpcType().equals(ReactiveGrpcMethodMetadata.CLIENT_STREAMING)) {
            Payload routePayload = ByteBufPayload.create(Unpooled.EMPTY_BUFFER, methodMetadata.getCompositeMetadataByteBuf().retainedDuplicate());
            Flux<Payload> paramsPayloadFlux = ((Flux<GeneratedMessageV3>) params[0]).map(param -> ByteBufPayload.create(Unpooled.wrappedBuffer(param.toByteArray()),
                    PayloadUtils.getCompositeMetaDataWithEncoding().retainedDuplicate()));
            return rsocketChannel(rsocket, paramsPayloadFlux.startWith(routePayload), methodMetadata.getInferredClassForReturn()).last();
        } else if (methodMetadata.getRpcType().equals(ReactiveGrpcMethodMetadata.BIDIRECTIONAL_STREAMING)) {
            Payload routePayload = ByteBufPayload.create(Unpooled.EMPTY_BUFFER, methodMetadata.getCompositeMetadataByteBuf().retainedDuplicate());
            Flux<Payload> paramsPayloadFlux = ((Flux<GeneratedMessageV3>) params[0]).map(param -> ByteBufPayload.create(Unpooled.wrappedBuffer(param.toByteArray()),
                    PayloadUtils.getCompositeMetaDataWithEncoding().retainedDuplicate()));
            return rsocketChannel(rsocket, paramsPayloadFlux.startWith(routePayload), methodMetadata.getInferredClassForReturn());
        }
        return Mono.error(new Exception(RsocketErrorCode.message("RST-611301")));
    }

}
