package com.alibaba.rsocket.gateway.grpc;

import com.alibaba.rsocket.gateway.grpc.impl.RSocketGrpcSupport;
import com.google.protobuf.GeneratedMessageV3;
import io.rsocket.RSocket;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.time.Duration;

import static com.alibaba.rsocket.reactive.ReactiveMethodSupport.getInferredClassForGeneric;

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

    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] params) {
        Mono<GeneratedMessageV3> monoParam = (Mono<GeneratedMessageV3>) params[0];
        if (method.getReturnType().isAssignableFrom(Mono.class)) {
            return monoParam
                    .map(param -> PayloadUtils.constructRequestPayload(param, service, method.getName()))
                    .flatMap(requestPayload -> rsocketRpc(rsocket, requestPayload, getInferredClassForGeneric(method.getGenericReturnType())).timeout(this.timeout));
        } else if (method.getReturnType().isAssignableFrom(Flux.class)) {
            return monoParam
                    .map(param -> PayloadUtils.constructRequestPayload(param, service, method.getName()))
                    .flatMapMany(requestPayload -> rsocketStream(rsocket, requestPayload, getInferredClassForGeneric(method.getGenericReturnType())).timeout(this.timeout));

        } else {
            return Mono.error(new Exception("error"));
        }

    }

}
