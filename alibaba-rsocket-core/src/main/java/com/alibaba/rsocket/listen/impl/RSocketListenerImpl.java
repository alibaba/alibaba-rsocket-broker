package com.alibaba.rsocket.listen.impl;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.listen.RSocketListener;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.plugins.DuplexConnectionInterceptor;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.local.LocalServerTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.TcpServer;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RSocket listener implementation
 *
 * @author leijuan
 */
public class RSocketListenerImpl implements RSocketListener {
    private Logger log = LoggerFactory.getLogger(RSocketListenerImpl.class);
    private Map<Integer, String> schemas = new HashMap<>();
    private String host = "0.0.0.0";
    private static final String[] protocols = new String[]{"TLSv1.3", "TLSv.1.2"};
    private Certificate certificate;
    private PrivateKey privateKey;
    private SocketAcceptor acceptor;
    private List<RSocketInterceptor> responderInterceptors = new ArrayList<>();
    private List<SocketAcceptorInterceptor> acceptorInterceptors = new ArrayList<>();
    private List<DuplexConnectionInterceptor> connectionInterceptors = new ArrayList<>();
    private Integer status = -1;
    private List<Disposable> responders = new ArrayList<>();

    public void host(String host) {
        this.host = host;
    }

    public void listen(String schema, int port) {
        this.schemas.put(port, schema);
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public void setAcceptor(SocketAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    public void addResponderInterceptor(RSocketInterceptor interceptor) {
        this.responderInterceptors.add(interceptor);
    }

    public void addSocketAcceptorInterceptor(SocketAcceptorInterceptor interceptor) {
        this.acceptorInterceptors.add(interceptor);
    }

    public void addConnectionInterceptor(DuplexConnectionInterceptor interceptor) {
        this.connectionInterceptors.add(interceptor);
    }

    @Override
    public Collection<String> serverUris() {
        return schemas.entrySet().stream()
                .map(entry -> entry.getValue() + "://0.0.0.0:" + entry.getKey())
                .collect(Collectors.toSet());
    }

    @Override
    public void start() throws Exception {
        if (status != 1) {
            for (Map.Entry<Integer, String> entry : schemas.entrySet()) {
                String schema = entry.getValue();
                int port = entry.getKey();
                ServerTransport<?> transport;
                if (schema.equals("local")) {
                    transport = LocalServerTransport.create("unittest");
                } else if (schema.equals("tcp")) {
                    transport = TcpServerTransport.create(host, port);
                } else if (schema.equals("tcps")) {
                    TcpServer tcpServer = TcpServer.create()
                            .host(host)
                            .port(port)
                            .secure(ssl -> ssl.sslContext(
                                    SslContextBuilder.forServer(privateKey, (X509Certificate) certificate)
                                            .protocols(protocols)
                                            .sslProvider(getSslProvider())
                            ));
                    transport = TcpServerTransport.create(tcpServer);
                } else if (schema.equals("ws")) {
                    transport = WebsocketServerTransport.create(host, port);
                } else if (schema.equals("wss")) {
                    HttpServer httpServer = HttpServer.create()
                            .host(host)
                            .port(port)
                            .secure(ssl -> ssl.sslContext(
                                    SslContextBuilder.forServer(privateKey, (X509Certificate) certificate)
                                            .protocols(protocols)
                                            .sslProvider(getSslProvider())
                            ));
                    transport = WebsocketServerTransport.create(httpServer);
                } else {
                    transport = TcpServerTransport.create(host, port);
                }
                RSocketServer rsocketServer = RSocketServer.create();
                //acceptor interceptor
                for (SocketAcceptorInterceptor acceptorInterceptor : acceptorInterceptors) {
                    rsocketServer.interceptors(interceptorRegistry -> {
                        interceptorRegistry.forSocketAcceptor(acceptorInterceptor);
                    });
                }
                //connection interceptor
                for (DuplexConnectionInterceptor connectionInterceptor : connectionInterceptors) {
                    rsocketServer.interceptors(interceptorRegistry -> {
                        interceptorRegistry.forConnection(connectionInterceptor);
                    });

                }
                //responder interceptor
                for (RSocketInterceptor responderInterceptor : responderInterceptors) {
                    rsocketServer.interceptors(interceptorRegistry -> {
                        interceptorRegistry.forResponder(responderInterceptor);
                    });
                }
                Disposable disposable = rsocketServer
                        .acceptor(acceptor)
                        .bind(transport)
                        .onTerminateDetach()
                        .subscribe();
                responders.add(disposable);
                log.info(RsocketErrorCode.message("RST-100001", schema + "://" + host + ":" + port));
            }
            status = 1;
            RSocketAppContext.rsocketPorts = schemas;
        }
    }

    @Override
    public void stop() throws Exception {
        for (Disposable responder : responders) {
            responder.dispose();
        }
        status = -1;
    }

    @Override
    public Integer getStatus() {
        return status;
    }

    private SslProvider getSslProvider() {
        if (OpenSsl.isAvailable()) {
            return SslProvider.OPENSSL_REFCNT;
        } else {
            return SslProvider.JDK;
        }
    }
}
