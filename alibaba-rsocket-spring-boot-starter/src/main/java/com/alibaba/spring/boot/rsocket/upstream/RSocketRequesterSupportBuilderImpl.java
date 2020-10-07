package com.alibaba.spring.boot.rsocket.upstream;

import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.spring.boot.rsocket.RSocketProperties;
import com.alibaba.spring.boot.rsocket.RSocketRequesterSupportImpl;
import io.rsocket.SocketAcceptor;
import io.rsocket.plugins.RSocketInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * RSocket Requester support  builder implementation
 *
 * @author leijuan
 */
public class RSocketRequesterSupportBuilderImpl implements RSocketRequesterSupportBuilder {
    private RSocketRequesterSupport requesterSupport;
    private List<RSocketInterceptor> requesterInterceptors = new ArrayList<>();
    private List<RSocketInterceptor> responderInterceptors = new ArrayList<>();

    public RSocketRequesterSupportBuilderImpl(RSocketProperties properties,
                                              Properties env,
                                              SocketAcceptor socketAcceptor) {
        this.requesterSupport = new RSocketRequesterSupportImpl(properties, env, socketAcceptor);
    }

    public RSocketRequesterSupportBuilder requesterSupport(RSocketRequesterSupport requesterSupport) {
        this.requesterSupport = requesterSupport;
        return this;
    }

    @Override
    public RSocketRequesterSupportBuilder addResponderInterceptor(RSocketInterceptor interceptor) {
        this.responderInterceptors.add(interceptor);
        return this;
    }

    @Override
    public RSocketRequesterSupportBuilder addRequesterInterceptor(RSocketInterceptor interceptor) {
        this.requesterInterceptors.add(interceptor);
        return this;
    }

    @Override
    public RSocketRequesterSupport build() {
        if (!this.responderInterceptors.isEmpty()) {
            this.requesterSupport.responderInterceptors().addAll(responderInterceptors);
        }
        if (!this.requesterInterceptors.isEmpty()) {
            this.requesterSupport.requestInterceptors().addAll(requesterInterceptors);
        }
        return this.requesterSupport;
    }
}
