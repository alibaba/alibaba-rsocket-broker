package com.alibaba.rsocket.gateway.grpc;

import com.alibaba.user.ReactorAccountServiceGrpc;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.spring.boot.rsocket.RSocketProperties;
import io.rsocket.RSocket;
import org.jetbrains.annotations.NotNull;
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
    public RSocket rsocketBroker(@NotNull UpstreamManager upstreamManager) {
        return upstreamManager.findBroker().getLoadBalancedRSocket();
    }

    @Bean
    public ReactorAccountServiceGrpc.AccountServiceImplBase grpcAccountService(@NotNull UpstreamManager upstreamManager,
                                                                               @NotNull RSocketProperties rsocketProperties) throws Exception {
        return GrpcServiceRSocketImplBuilder
                .stub(ReactorAccountServiceGrpc.AccountServiceImplBase.class)
                .upstreamManager(upstreamManager)
                .timeoutMillis(rsocketProperties.getTimeout())
                .build();
    }
}
