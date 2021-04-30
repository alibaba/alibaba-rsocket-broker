package com.alibaba.user.springboot;

import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.user.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * User services auto configuration for Spring Boot
 *
 * @author leijuan
 */
@Configuration(proxyBeanMethods = false)
public class UserServicesAutoConfiguration {

    @Bean
    public UserService userService(UpstreamManager upstreamManager) {
        return RSocketRemoteServiceBuilder
                .client(UserService.class)
                .upstreamManager(upstreamManager)
                .build();
    }

    @Bean
    public UserService2 userService2(UpstreamManager upstreamManager) {
        return RSocketRemoteServiceBuilder
                .client(UserService2.class)
                .upstreamManager(upstreamManager)
                .build();
    }


    @Bean
    public RxUserService rxUserService(UpstreamManager upstreamManager) {
        return RSocketRemoteServiceBuilder
                .client(RxUserService.class)
                .encodingType(RSocketMimeType.CBOR)
                .upstreamManager(upstreamManager)
                .build();
    }

    @Bean
    public Rx3UserService rx3UserService(UpstreamManager upstreamManager) {
        return RSocketRemoteServiceBuilder
                .client(Rx3UserService.class)
                .upstreamManager(upstreamManager)
                .build();
    }

    @Bean
    public AccountService accountService(UpstreamManager upstreamManager) {
        return RSocketRemoteServiceBuilder
                .client(AccountService.class)
                .encodingType(RSocketMimeType.Protobuf)
                .upstreamManager(upstreamManager)
                .build();
    }

}
