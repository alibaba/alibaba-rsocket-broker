package com.alibaba.rsocket.listen;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import reactor.core.publisher.Mono;

/**
 * rsocket responder handler factory
 *
 * @author leijuan
 */
@FunctionalInterface
public interface RSocketResponderHandlerFactory extends SocketAcceptor {

    Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket);
}
