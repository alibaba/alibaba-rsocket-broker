package com.alibaba.rsocket.transport;

import org.junit.jupiter.api.Test;

import static com.alibaba.rsocket.transport.NetworkUtil.isInternalIp;
import static org.assertj.core.api.Assertions.assertThat;

public class NetworkUtilTest {

    @Test
    public void testIsInternalIp() {
        assertThat(isInternalIp("192.168.11.11")).isTrue();
        assertThat(isInternalIp("127.0.0.1")).isTrue();
        assertThat(isInternalIp("localhost")).isTrue();
        assertThat(isInternalIp("taobao.com")).isFalse();
    }
}
