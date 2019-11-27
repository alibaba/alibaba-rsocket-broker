package com.alibaba.spring.boot.rsocket.upstream;

import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.spring.boot.rsocket.RSocketProperties;
import com.alibaba.spring.boot.rsocket.RSocketRequesterSupportImpl;
import io.rsocket.SocketAcceptor;
import io.rsocket.plugins.RSocketInterceptor;

import java.util.Properties;

/**
 * RSocket Requester support  builder implementation
 *
 * @author leijuan
 */
public class RSocketRequesterSupportBuilderImpl implements RSocketRequesterSupportBuilder {
    private RSocketRequesterSupportImpl requesterSupport;

    public RSocketRequesterSupportBuilderImpl(RSocketProperties properties,
                                              Properties env,
                                              SocketAcceptor socketAcceptor) {
        this.requesterSupport = new RSocketRequesterSupportImpl(properties, env, socketAcceptor);
    }

    @Override
    public RSocketRequesterSupportBuilder addResponderInterceptor(RSocketInterceptor interceptor) {
        return this;
    }

    @Override
    public RSocketRequesterSupportBuilder addRequesterInterceptor(RSocketInterceptor interceptor) {
        return this;
    }

    @Override
    public RSocketRequesterSupport build() {
        return requesterSupport;
    }
}
