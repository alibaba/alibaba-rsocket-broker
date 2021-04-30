package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import com.alibaba.rsocket.loadbalance.LoadBalancedRSocket;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.spring.boot.rsocket.demo.controllers.user.UserServiceExtra;
import com.alibaba.spring.boot.rsocket.hessian.HessianDecoder;
import com.alibaba.spring.boot.rsocket.hessian.HessianEncoder;
import com.alibaba.user.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;

/**
 * Service consumer configuration
 *
 * @author leijuan
 */
@Configuration
public class ServiceConsumeConfiguration {

    @Bean
    public UserServiceExtra userServiceExtra(UpstreamManager upstreamManager) {
        return RSocketRemoteServiceBuilder
                .client(UserServiceExtra.class)
                .service(UserService.class.getCanonicalName())
                .upstreamManager(upstreamManager)
                .acceptEncodingType(RSocketMimeType.Json)
                .build();
    }

    @Bean
    public RSocketRequester rsocketRequester(UpstreamManager upstreamManager) {
        LoadBalancedRSocket loadBalancedRSocket = upstreamManager.findBroker().getLoadBalancedRSocket();
        RSocketStrategies rSocketStrategies = RSocketStrategies.builder()
                .encoder(new HessianEncoder())
                .decoder(new HessianDecoder())
                .build();
        return RSocketRequester.wrap(loadBalancedRSocket,
                MimeType.valueOf("application/x-hessian"),
                MimeType.valueOf("message/x.rsocket.composite-metadata.v0"),
                rSocketStrategies);
    }
}
