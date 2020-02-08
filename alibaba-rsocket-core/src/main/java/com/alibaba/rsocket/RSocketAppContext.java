package com.alibaba.rsocket;

import com.alibaba.rsocket.metadata.RSocketMimeType;

import java.util.UUID;

/**
 * RSocket application context: exposed service, requested service, global information
 *
 * @author leijuan
 */
public class RSocketAppContext {
    public static final String ID = UUID.randomUUID().toString();
    public static final String DEFAULT_DATA_TYPE = RSocketMimeType.Hessian.getType();
    public static final Integer LISTEN_PORT = 42252;
}
