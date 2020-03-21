package com.alibaba.rsocket.gateway.grpc;

import com.alibaba.account.ReactorAccountServiceGrpc;
import com.alibaba.rsocket.upstream.UpstreamManager;
import io.rsocket.RSocket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC Service Configuration
 *
 * @author leijuan
 */
@Configuration
public class GrpcServiceConfiguration {

    @Bean
    public RSocket rsocketBroker(UpstreamManager upstreamManager) {
        return upstreamManager.findBroker().getLoadBalancedRSocket();
    }

    @Bean
    public ReactorAccountServiceGrpc.AccountServiceImplBase grpcAccountService(UpstreamManager upstreamManager) throws Exception {
        return GrpcServiceRSocketImplBuilder
                .stub(ReactorAccountServiceGrpc.AccountServiceImplBase.class)
                .upstreamManager(upstreamManager)
                .build();
    }
}
