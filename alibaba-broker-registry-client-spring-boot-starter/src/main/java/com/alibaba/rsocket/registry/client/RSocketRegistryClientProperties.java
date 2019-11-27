package com.alibaba.rsocket.registry.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * RSocket registry client properties
 *
 * @author leijuan
 */
@ConfigurationProperties(prefix = "rsocket.registry")
public class RSocketRegistryClientProperties {
    /**
     * rsocket broker list
     */
    private List<String> brokers;

    public List<String> getBrokers() {
        return brokers;
    }

    public void setBrokers(List<String> brokers) {
        this.brokers = brokers;
    }
}
