package com.alibaba.rsocket.gateway.grpc;

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
}
