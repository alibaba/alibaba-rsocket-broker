package com.alibaba.rsocket.cloudevents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

/**
 * Cloud RSocket Test
 *
 * @author leijuan
 */
public class CloudEventRSocketTest {

    @Test
    public void testConstructReplayPayload() {
        URI replyTo = URI.create("rsocket:///REQUEST_FNF/com.xxxx.XxxService#configEventAck");
        String path = replyTo.getPath();
        String serviceName = path.substring(path.lastIndexOf("/") + 1);
        String method = replyTo.getFragment();
        Assertions.assertEquals(serviceName, "com.xxxx.XxxService");
        Assertions.assertEquals(method, "configEventAck");

    }
}
