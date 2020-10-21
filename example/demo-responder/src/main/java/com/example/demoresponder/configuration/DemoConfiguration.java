package com.example.demoresponder.configuration;

import com.ablibaba.demo.EchoService;
import com.ablibaba.demo.ProfileService;
import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.example.demoresponder.service.EchoServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author hupeiD
 */
@Configuration
public class DemoConfiguration {

    @Bean
    public ProfileService profileService(UpstreamManager upstreamManager) {
        return RSocketRemoteServiceBuilder.client(ProfileService.class)
                                          .upstreamManager(upstreamManager)
                                          .service(ProfileService.class.getCanonicalName())
                                          .build();
    }

    @Bean
    public EchoService echoService() {
        return new EchoServiceImpl();
    }
}
