package com.alibaba.rsocket.broker.config;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableDiscoveryClient
@Profile("kubernetes")
public class KubernetesConfiguration {

}
