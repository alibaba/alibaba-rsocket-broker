package com.alibaba.rsocket.client;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.rsocket.metadata.BearerTokenMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.ServiceRegistryMetadata;
import com.alibaba.rsocket.observability.MetricsService;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.rpc.RSocketResponderHandler;
import com.alibaba.rsocket.rpc.ReactiveServiceDiscovery;
import com.alibaba.rsocket.transport.NetworkUtil;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.SocketAcceptor;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Simple RSocketRequesterSupport for App
 *
 * @author leijuan
 */
@SuppressWarnings("rawtypes")
public class SimpleRSocketRequesterSupport implements RSocketRequesterSupport {
    private char[] jwtToken;
    private List<String> brokers;
    private String appName;
    private String topology = "intranet";
    private Map<String, String> metadata;
    private LocalReactiveServiceCaller serviceCaller;
    private Sinks.Many<CloudEventImpl> eventProcessor;

    public SimpleRSocketRequesterSupport(String appName, char[] jwtToken, List<String> brokers,
                                         LocalReactiveServiceCaller serviceCaller,
                                         Sinks.Many<CloudEventImpl> eventProcessor) {
        this.appName = appName;
        this.jwtToken = jwtToken;
        this.brokers = brokers;
        this.eventProcessor = eventProcessor;
        this.serviceCaller = serviceCaller;
    }

    public void setTopology(String topology) {
        this.topology = topology;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    public URI originUri() {
        return URI.create("tcp://" + NetworkUtil.LOCAL_IP + "?appName=" + appName + "&uuid=" + RSocketAppContext.ID);
    }

    @Override
    public Supplier<Payload> setupPayload(String serviceId) {
        return () -> {
            //composite metadata with app metadata
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(getAppMetadata());
            //authentication for broker
            if (serviceId.equals("*")) {
                if (this.jwtToken != null && this.jwtToken.length > 0) {
                    compositeMetadata.addMetadata(new BearerTokenMetadata(this.jwtToken));
                }
            }
            Set<ServiceLocator> serviceLocators = exposedServices().get();
            if (!serviceLocators.isEmpty()) {
                ServiceRegistryMetadata serviceRegistryMetadata = new ServiceRegistryMetadata();
                serviceRegistryMetadata.setPublished(serviceLocators);
                compositeMetadata.addMetadata(serviceRegistryMetadata);
            }
            return ByteBufPayload.create(Unpooled.EMPTY_BUFFER, compositeMetadata.getContent());
        };
    }

    @Override
    public Supplier<Set<ServiceLocator>> exposedServices() {
        Set<String> allServices = this.serviceCaller.findAllServices();
        if (!allServices.isEmpty()) {
            return () -> allServices.stream()
                    .filter(serviceName -> !serviceName.equals(ReactiveServiceDiscovery.class.getCanonicalName())
                            && !serviceName.equals(RSocketServiceHealth.class.getCanonicalName())
                            && !serviceName.equals(MetricsService.class.getCanonicalName()))
                    .map(serviceName -> new ServiceLocator("", serviceName, ""))
                    .collect(Collectors.toSet());
        }
        return Collections::emptySet;
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
            return ServicesExposedEvent.convertServicesToCloudEvent(serviceLocators);
        };
    }

    @Override
    public SocketAcceptor socketAcceptor() {
        return (setupPayload, requester) -> Mono.fromCallable(() -> new RSocketResponderHandler(serviceCaller, eventProcessor, requester, setupPayload));
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
        appMetadata.setTopology(this.topology);
        appMetadata.setMetadata(this.metadata);
        return appMetadata;
    }
}
