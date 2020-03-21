package com.alibaba.rsocket.gateway.grpc;

import com.alibaba.account.ReactorAccountServiceGrpc;
import com.alibaba.rsocket.upstream.UpstreamManager;
import io.rsocket.RSocket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RSocket Service Configuration
 *
 * @author leijuan
 */
@Configuration
public class RSocketServiceConfiguration {

    @Bean
    public RSocket rsocketBroker(UpstreamManager upstreamManager) {
        return upstreamManager.findBroker().getLoadBalancedRSocket();
    }

    @Bean
    public ReactorAccountServiceGrpc.AccountServiceImplBase userService(UpstreamManager upstreamManager) throws Exception {
        return GrpcServiceRSocketImplBuilder
                .stub(ReactorAccountServiceGrpc.AccountServiceImplBase.class, "com.alibaba.account.AccountService")
                .upstreamManager(upstreamManager)
                .build();
    }
}
