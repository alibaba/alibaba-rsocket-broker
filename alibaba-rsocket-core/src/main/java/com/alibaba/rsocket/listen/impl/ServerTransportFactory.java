package com.alibaba.rsocket.listen.impl;

import io.netty.handler.ssl.SslContextBuilder;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.local.LocalServerTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.TcpServer;

public abstract class ServerTransportFactory {
    public abstract ServerTransport<?> create(String host, int port, SslContextBuilder sslContextBuilder);

    public static ServerTransportFactory getFactory(String schema) {
        switch (schema) {
            case "local":
                return new LocalServerTransportFactory();
            case "tcp":
                return new TcpServerTransportFactory();
            case "tcps":
                return new TcpsServerTransportFactory();
            case "ws":
                return new WebsocketServerTransportFactory();
            case "wss":
                return new WssServerTransportFactory();
            default:
                return new TcpServerTransportFactory();
        }
    }

    private static class LocalServerTransportFactory extends ServerTransportFactory {

        @Override
        public ServerTransport<?> create(String host, int port, SslContextBuilder sslContextBuilder) {
            return LocalServerTransport.create("unittest");
        }
    }

    private static class TcpServerTransportFactory extends ServerTransportFactory {
        @Override
        public ServerTransport<?> create(String host, int port, SslContextBuilder sslContextBuilder) {
            return TcpServerTransport.create(host, port);
        }
    }

    private static class TcpsServerTransportFactory extends ServerTransportFactory {
        @Override
        public ServerTransport<?> create(String host, int port, SslContextBuilder sslContextBuilder) {
            TcpServer tcpServer = TcpServer.create()
                    .host(host)
                    .port(port)
                    .secure(ssl -> ssl.sslContext(sslContextBuilder));
            return TcpServerTransport.create(tcpServer);
        }
    }

    private static class WebsocketServerTransportFactory extends ServerTransportFactory {

        @Override
        public ServerTransport<?> create(String host, int port, SslContextBuilder sslContextBuilder) {
            return WebsocketServerTransport.create(host, port);
        }
    }

    private static class WssServerTransportFactory extends ServerTransportFactory {

        @Override
        public ServerTransport<?> create(String host, int port, SslContextBuilder sslContextBuilder) {
            HttpServer httpServer = HttpServer.create()
                    .host(host)
                    .port(port)
                    .secure(ssl -> ssl.sslContext(sslContextBuilder));
            return WebsocketServerTransport.create(httpServer);
        }
    }
}



