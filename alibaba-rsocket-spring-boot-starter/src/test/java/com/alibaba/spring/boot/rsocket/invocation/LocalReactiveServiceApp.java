package com.alibaba.spring.boot.rsocket.invocation;

import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

/**
 * local reactive service app
 *
 * @author leijuan
 */
@SpringBootApplication
public class LocalReactiveServiceApp {

    @Bean(name = "reactiveTestService")
    @DependsOn("rSocketListener")
    public ReactiveTestService reactiveTestService(UpstreamManager upstreamManager) {
        return RSocketRemoteServiceBuilder
                .client(ReactiveTestService.class)
                .upstreamManager(upstreamManager)
                .build();
    }
}
