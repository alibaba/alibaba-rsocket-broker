package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.account.AccountService;
import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.user.Rx3UserService;
import com.alibaba.user.RxUserService;
import com.alibaba.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ReactiveAdapterRegistry;

import javax.annotation.PostConstruct;

/**
 * Service consumer configuration
 *
 * @author leijuan
 */
@Configuration
public class ServiceConsumeConfiguration {
    @Autowired
    private ReactiveAdapterRegistry reactiveAdapterRegistry;

    @PostConstruct
    public void init() {
        new RxJava3Registrar().registerAdapters(reactiveAdapterRegistry);
    }

    @Bean
    public UserService userService(UpstreamManager upstreamManager) {
        return RSocketRemoteServiceBuilder
                .client(UserService.class)
                .upstreamManager(upstreamManager)
                //.endpoint("ip:192.168.1.2") //for testing
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
