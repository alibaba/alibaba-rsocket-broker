package com.alibaba.rsocket.broker.config;

import com.alibaba.spring.boot.rsocket.broker.RSocketBrokerProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class ConfigurationServiceRedisImplTest {
    private static ConfigurationServiceRedisImpl redisConfig;

    @BeforeAll
    public static void setUp() {
        RSocketBrokerProperties properties = new RSocketBrokerProperties();
        properties.setConfigStore("redis://localhost/0");
        redisConfig = new ConfigurationServiceRedisImpl(properties);
    }

    @Test
    public void testPut() {
        String key = "app2:owner";
        String value = redisConfig.put(key, "leijuan")
                .then(redisConfig.get(key)).block();
        System.out.println(value);
    }

    @Test
    public void testListGroups() {
        redisConfig.getGroups().doOnNext(System.out::println).blockLast();
    }
}
