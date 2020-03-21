package com.alibaba.rsocket.gateway.grpc.impl;

import com.alibaba.account.Account;
import com.alibaba.account.ReactorAccountServiceGrpc;
import com.alibaba.rsocket.ServiceMapping;
import com.alibaba.rsocket.gateway.grpc.PayloadUtils;
import com.google.protobuf.Int32Value;
import io.rsocket.RSocket;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * account service gRPC reactive implementation with RSocket as backend
 *
 * @author leijuan
 */
@GRpcService
@ServiceMapping(value = "com.alibaba.account.AccountService")
public class AccountServiceGrpReactiveImpl extends ReactorAccountServiceGrpc.AccountServiceImplBase implements RSocketGrpcSupport {
    @Autowired
    private RSocket rsocket;
    private static final String SERVICE_NAME = "com.alibaba.account.AccountService";

    @Override
    public Mono<Account> findById(Mono<Int32Value> request) {
        return request
                .map(id -> PayloadUtils.constructRequestPayload(id, SERVICE_NAME, "findById"))
                .flatMap(requestPayload -> rsocketRpc(rsocket, requestPayload, Account.class));
    }

    @Override
    public Flux<Account> findByStatus(Mono<Int32Value> request) {
        return request
                .map(id -> PayloadUtils.constructRequestPayload(id, SERVICE_NAME, "findByStatus"))
                .flatMapMany(requestPayload -> rsocketStream(rsocket, requestPayload, Account.class));
    }


}
