package com.alibaba.spring.boot.rsocket.broker;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.transport.NetworkUtil;

import java.net.URI;

/**
 * Broker App Context
 *
 * @author leijuan
 */
public class BrokerAppContext {
    private static URI SOURCE = URI.create("broker://" + NetworkUtil.LOCAL_IP + "/" + "?id=" + RSocketAppContext.ID);

    public static URI identity() {
        return SOURCE;
    }
}
