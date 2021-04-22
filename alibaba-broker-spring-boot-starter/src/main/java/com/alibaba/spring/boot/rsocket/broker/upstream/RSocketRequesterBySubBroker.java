package com.alibaba.spring.boot.rsocket.broker.upstream;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.rsocket.metadata.BearerTokenMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.route.RSocketFilterChain;
import com.alibaba.rsocket.transport.NetworkUtil;
import com.alibaba.spring.boot.rsocket.broker.BrokerAppContext;
import com.alibaba.spring.boot.rsocket.broker.RSocketBrokerProperties;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.SocketAcceptor;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.util.ByteBufPayload;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * RSocket Requester by sub Broker
 *
 * @author leijuan
 */
public class RSocketRequesterBySubBroker implements RSocketRequesterSupport {
    private RSocketBrokerProperties properties;
    private Environment env;
    private String appName;
    private char[] jwtToken;
    private ServiceRoutingSelector serviceRoutingSelector;
    private RSocketBrokerHandlerRegistry handlerRegistry;
    private RSocketFilterChain filterChain;
    private UpstreamBrokerCluster upstreamBrokerCluster;

    public RSocketRequesterBySubBroker(RSocketBrokerProperties properties, Environment env,
                                       RSocketBrokerHandlerRegistry handlerRegistry,
                                       RSocketFilterChain filterChain,
                                       ServiceRoutingSelector serviceRoutingSelector) {
        this.env = env;
        this.appName = env.getProperty("spring.application.name", env.getProperty("application.name", "unknown-app"));
        this.jwtToken = env.getProperty("rsocket.jwt-token", "").toCharArray();
        this.serviceRoutingSelector = serviceRoutingSelector;
        this.handlerRegistry = handlerRegistry;
        this.filterChain = filterChain;
        this.properties = properties;
    }

    public void setUpstreamBrokerCluster(UpstreamBrokerCluster upstreamBrokerCluster) {
        this.upstreamBrokerCluster = upstreamBrokerCluster;
    }

    @Override
    public URI originUri() {
        return URI.create("tcp://" + NetworkUtil.LOCAL_IP + ":" + properties.getListen()
                + "?appName=" + appName
                + "&uuid=" + RSocketAppContext.ID);
    }

    @Override
    public Supplier<Payload> setupPayload() {
        return () -> {
            //composite metadata with app metadata
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(getAppMetadata());
            // authentication
            if (this.jwtToken != null && this.jwtToken.length > 0) {
                compositeMetadata.addMetadata(new BearerTokenMetadata(this.jwtToken));
            }
            return ByteBufPayload.create(Unpooled.EMPTY_BUFFER, compositeMetadata.getContent());
        };
    }

    @Override
    public Supplier<Set<ServiceLocator>> exposedServices() {
        return () -> {
            return serviceRoutingSelector.findAllServices().stream()
                    .filter(serviceLocator -> serviceLocator.hasTag("global"))
                    .collect(Collectors.toSet());
        };
    }

    @Override
    public Supplier<Set<ServiceLocator>> subscribedServices() {
        return Collections::emptySet;
    }

    @Override
    public Supplier<CloudEventImpl<ServicesExposedEvent>> servicesExposedEvent() {
        return () -> {
            Collection<ServiceLocator> serviceLocators = exposedServices().get();
            if (serviceLocators.isEmpty()) return null;
            ServicesExposedEvent servicesExposedEvent = new ServicesExposedEvent();
            for (ServiceLocator serviceLocator : serviceLocators) {
                servicesExposedEvent.addService(serviceLocator);
            }
            servicesExposedEvent.setAppId(RSocketAppContext.ID);
            return RSocketCloudEventBuilder.builder(servicesExposedEvent)
                    .withSource(BrokerAppContext.identity())
                    .build();
        };
    }

    @Override
    public SocketAcceptor socketAcceptor() {
        return (connectionSetupPayload, rsocket) -> Mono.just(new UpstreamForwardRSocket(this.serviceRoutingSelector, rsocket, this.filterChain, this.handlerRegistry, this.upstreamBrokerCluster));
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
        System.out.println("app metadata");
        //app metadata
        AppMetadata appMetadata = new AppMetadata();
        appMetadata.setUuid(RSocketAppContext.ID);
        appMetadata.setName(appName);
        appMetadata.setIp(NetworkUtil.LOCAL_IP);
        appMetadata.setDevice("SpringBootApp");
        appMetadata.setRsocketPorts(RSocketAppContext.rsocketPorts);
        //brokers
        appMetadata.setBrokers(properties.getUpstreamBrokers());
        appMetadata.setTopology(properties.getTopology());
        appMetadata.setRsocketPorts(RSocketAppContext.rsocketPorts);
        //web port
        appMetadata.setWebPort(Integer.parseInt(env.getProperty("server.port", "0")));
        appMetadata.setManagementPort(appMetadata.getWebPort());
        //management port
        if (env.getProperty("management.server.port") != null) {
            appMetadata.setManagementPort(Integer.parseInt(env.getProperty("management.server.port", "0")));
        }
        appMetadata.setManagementPort(Integer.parseInt(env.getProperty("management.server.port", "" + appMetadata.getWebPort())));
        appMetadata.addMetadata("broker", "true");
        return appMetadata;
    }
}
