package com.alibaba.rsocket;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import io.rsocket.Payload;
import io.rsocket.SocketAcceptor;
import io.rsocket.plugins.RSocketInterceptor;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * RSocket requester support: setup payload, exposed services, acceptor, plugins
 *
 * @author leijuan
 */
public interface RSocketRequesterSupport {

    URI originUri();

    Supplier<Payload> setupPayload();

    Supplier<Set<ServiceLocator>> exposedServices();

    Supplier<Set<ServiceLocator>> subscribedServices();

    Supplier<CloudEventImpl<ServicesExposedEvent>> servicesExposedEvent();

    SocketAcceptor socketAcceptor();

    List<RSocketInterceptor> responderInterceptors();

    List<RSocketInterceptor> requestInterceptors();
}
