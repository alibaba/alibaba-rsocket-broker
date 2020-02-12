package com.alibaba.rsocket.listen.impl;

import com.alibaba.rsocket.listen.RSocketListener;
import io.rsocket.SocketAcceptor;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.plugins.DuplexConnectionInterceptor;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.plugins.SocketAcceptorInterceptor;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.function.Consumer;

/**
 * RSocket listener builder implementation
 *
 * @author leijuan
 */
public class RSocketListenerBuilderImpl implements RSocketListener.Builder {
    private RSocketListenerImpl rSocketListener = new RSocketListenerImpl();

    @Override
    public RSocketListener.Builder host(String host) {
        rSocketListener.host(host);
        return this;
    }

    @Override
    public RSocketListener.Builder listen(String schema, int port) {
        rSocketListener.listen(schema, port);
        return this;
    }

    @Override
    public RSocketListener.Builder errorConsumer(Consumer<Throwable> errorConsumer) {
        rSocketListener.errorConsumer(errorConsumer);
        return this;
    }

    @Override
    public RSocketListener.Builder sslContext(Certificate certificate, PrivateKey privateKey) {
        rSocketListener.setCertificate(certificate);
        rSocketListener.setPrivateKey(privateKey);
        return this;
    }

    @Override
    public RSocketListener.Builder payloadDecoder(PayloadDecoder payloadDecoder) {
        rSocketListener.setPayloadDecoder(payloadDecoder);
        return this;
    }

    @Override
    public RSocketListener.Builder addResponderInterceptor(RSocketInterceptor interceptor) {
        rSocketListener.addResponderInterceptor(interceptor);
        return this;
    }

    @Override
    public RSocketListener.Builder addSocketAcceptorInterceptor(SocketAcceptorInterceptor interceptor) {
        rSocketListener.addSocketAcceptorInterceptor(interceptor);
        return this;
    }

    @Override
    public RSocketListener.Builder addConnectionInterceptor(DuplexConnectionInterceptor interceptor) {
        rSocketListener.addConnectionInterceptor(interceptor);
        return this;
    }

    @Override
    public RSocketListener.Builder acceptor(SocketAcceptor acceptor) {
        rSocketListener.setAcceptor(acceptor);
        return this;
    }

    @Override
    public RSocketListener build() {
        return rSocketListener;
    }
}
