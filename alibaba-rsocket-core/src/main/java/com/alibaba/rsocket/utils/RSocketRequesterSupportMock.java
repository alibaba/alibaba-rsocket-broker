package com.alibaba.rsocket.utils;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.rsocket.metadata.BearerTokenMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.transport.NetworkUtil;
import io.cloudevents.v1.CloudEventImpl;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.SocketAcceptor;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.util.ByteBufPayload;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * RSocketRequesterSupport Mock for unit testing
 *
 * @author leijuan
 */
public class RSocketRequesterSupportMock implements RSocketRequesterSupport {
    private char[] jwtToken;
    private List<String> brokers;
    private String appName = "MockApp";

    public RSocketRequesterSupportMock(String jwtToken, List<String> brokers) {
        this.jwtToken = jwtToken.toCharArray();
        this.brokers = brokers;
    }

    @Override
    public URI originUri() {
        return URI.create("tcp://" + NetworkUtil.LOCAL_IP + "?appName=" + appName + "&uuid=" + RSocketAppContext.ID);
    }

    @Override
    public Supplier<Payload> setupPayload() {
        //noinspection DuplicatedCode
        return () -> {
            //composite metadata with app metadata
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(getAppMetadata());
            if (this.jwtToken != null && this.jwtToken.length > 0) {
                compositeMetadata.addMetadata(new BearerTokenMetadata(this.jwtToken));
            }
            return ByteBufPayload.create(Unpooled.EMPTY_BUFFER, compositeMetadata.getContent());
        };
    }

    @Override
    public Supplier<Set<ServiceLocator>> exposedServices() {
        return Collections::emptySet;
    }

    @Override
    public Supplier<Set<ServiceLocator>> subscribedServices() {
        return Collections::emptySet;
    }

    @Override
    public Supplier<CloudEventImpl<ServicesExposedEvent>> servicesExposedEvent() {
        return () -> null;
    }

    @Override
    public SocketAcceptor socketAcceptor() {
        return null;
    }

    @Override
    public List<RSocketInterceptor> responderInterceptors() {
        return Collections.emptyList();
    }

    @Override
    public List<RSocketInterceptor> requestInterceptors() {
        return Collections.emptyList();
    }

    private AppMetadata getAppMetadata() {
        //app metadata
        AppMetadata appMetadata = new AppMetadata();
        appMetadata.setUuid(RSocketAppContext.ID);
        appMetadata.setName(appName);
        appMetadata.setIp(NetworkUtil.LOCAL_IP);
        appMetadata.setDevice(appName);
        appMetadata.setBrokers(brokers);
        return appMetadata;
    }
}
