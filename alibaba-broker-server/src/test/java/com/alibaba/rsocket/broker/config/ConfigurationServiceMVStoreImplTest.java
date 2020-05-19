package com.alibaba.rsocket.broker.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * ConfigurationServiceMVStoreImpl unit tests
 *
 * @author leijuan
 */
public class ConfigurationServiceMVStoreImplTest {
    private static String appName = "rsocket-config-client";
    private static ConfigurationServiceMVStoreImpl configurationService = new ConfigurationServiceMVStoreImpl();

    @BeforeAll
    public static void setUp() {

    }

    @AfterAll
    public static void tearDown() {
        configurationService.close();
    }

    @Test
    public void testOperation() throws Exception {
        String key = appName + ":" + "application.properties";
        configurationService.watch(key).subscribe(s -> {
            System.out.println("updated: " + s);
        });
        configurationService.put(key, "nick=leijuan").subscribe();
        Thread.sleep(1000);
        configurationService.put(key, "nick=leijuan2").subscribe();
        Thread.sleep(1000);
        Assertions.assertEquals(configurationService.get(key).block(), "nick=leijuan2");
    }
}
