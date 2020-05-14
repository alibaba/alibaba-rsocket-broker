package com.alibaba.rsocket.listen.impl;

import com.alibaba.rsocket.listen.RSocketListener;
import io.rsocket.SocketAcceptor;
import io.rsocket.plugins.DuplexConnectionInterceptor;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.plugins.SocketAcceptorInterceptor;

import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * RSocket listener builder implementation
 *
 * @author leijuan
 */
public class RSocketListenerBuilderImpl implements RSocketListener.Builder {
    private RSocketListenerImpl rsocketListener = new RSocketListenerImpl();

    @Override
    public RSocketListener.Builder host(String host) {
        rsocketListener.host(host);
        return this;
    }

    @Override
    public RSocketListener.Builder listen(String schema, int port) {
        rsocketListener.listen(schema, port);
        return this;
    }

    @Override
    public RSocketListener.Builder sslContext(Certificate certificate, PrivateKey privateKey) {
        rsocketListener.setCertificate(certificate);
        rsocketListener.setPrivateKey(privateKey);
        return this;
    }

    @Override
    public RSocketListener.Builder addResponderInterceptor(RSocketInterceptor interceptor) {
        rsocketListener.addResponderInterceptor(interceptor);
        return this;
    }

    @Override
    public RSocketListener.Builder addSocketAcceptorInterceptor(SocketAcceptorInterceptor interceptor) {
        rsocketListener.addSocketAcceptorInterceptor(interceptor);
        return this;
    }

    @Override
    public RSocketListener.Builder addConnectionInterceptor(DuplexConnectionInterceptor interceptor) {
        rsocketListener.addConnectionInterceptor(interceptor);
        return this;
    }

    @Override
    public RSocketListener.Builder acceptor(SocketAcceptor acceptor) {
        rsocketListener.setAcceptor(acceptor);
        return this;
    }

    @Override
    public RSocketListener build() {
        return rsocketListener;
    }
}
