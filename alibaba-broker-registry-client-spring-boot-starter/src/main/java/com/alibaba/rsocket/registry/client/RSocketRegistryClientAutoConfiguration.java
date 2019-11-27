package com.alibaba.rsocket.registry.client;

import com.alibaba.rsocket.discovery.DiscoveryService;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RSocket registry client auto configuration
 *
 * @author leijuan
 */
@Configuration
@EnableConfigurationProperties(RSocketRegistryClientProperties.class)
public class RSocketRegistryClientAutoConfiguration {

    @Bean
    public DiscoveryService rsocketDiscoveryService(UpstreamManager upstreamManager) {
        return RSocketRemoteServiceBuilder
                .client(DiscoveryService.class)
                .upstreamManager(upstreamManager)
                .build();
    }

    @Bean
    public ReactiveDiscoveryClient rsocketDiscoveryClient(DiscoveryService discoveryService) {
        return new RSocketDiscoveryClient(discoveryService);
    }
}
