package com.alibaba.rsocket.gateway.grpc;

import com.alibaba.account.Account;
import com.google.protobuf.Int32Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

/**
 * Reactive gRPC method metadata test
 *
 * @author leijuan
 */
public class ReactiveGrpcMethodMetadataTest {

    @Test
    public void testCreate() {
        ReactiveGrpcMethodMetadata methodMetadata = new ReactiveGrpcMethodMetadata(getMethod("findById"), "", "com.alibaba.account.AccountService", "");
        Assertions.assertEquals(ReactiveGrpcMethodMetadata.UNARY, methodMetadata.getRpcType());
        Assertions.assertEquals(methodMetadata.getInferredClassForReturn(), Account.class);
    }

    private Method getMethod(String name) {
        for (Method method : this.getClass().getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new RuntimeException("method not found:" + name);
    }

    public Mono<Account> findById(Mono<Int32Value> request) {
        return Mono.empty();
    }

    public Flux<Account> findByStatus(Mono<Int32Value> request) {
        return Flux.empty();
    }

    public Flux<Account> findByIdStream(Flux<Int32Value> request) {
        return Flux.empty();
    }


}
