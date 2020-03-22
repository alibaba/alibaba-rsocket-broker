package com.alibaba.rsocket.client.demo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * config fetch test
 *
 * @author leijuan
 */
@Disabled
public class ConfigFetchTest extends ConfigBaseTestCase {
    @Value("${developer}")
    private String developer;

    @Test
    public void testPrintDeveloper() {
        assertThat(developer).isNotNull();
        System.out.println(developer);
    }
}
