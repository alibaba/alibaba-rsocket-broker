package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.RSocketService;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import com.alibaba.rsocket.metadata.*;
import com.alibaba.rsocket.observability.MetricsService;
import com.alibaba.rsocket.transport.NetworkUtil;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.SocketAcceptor;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.util.ByteBufPayload;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * RSocket Requester Support implementation: setup payload, published service, token and app info
 *
 * @author leijuan
 */
public class RSocketRequesterSupportImpl implements RSocketRequesterSupport, ApplicationContextAware {
    protected Properties env;
    protected RSocketProperties properties;
    protected String appName;
    protected char[] jwtToken;
    protected ApplicationContext applicationContext;
    protected SocketAcceptor socketAcceptor;
    protected List<RSocketInterceptor> responderInterceptors = new ArrayList<>();
    protected List<RSocketInterceptor> requestInterceptors = new ArrayList<>();

    public RSocketRequesterSupportImpl(RSocketProperties properties, Properties env,
                                       SocketAcceptor socketAcceptor) {
        this.properties = properties;
        this.env = env;
        this.appName = env.getProperty("spring.application.name", env.getProperty("application.name"));
        this.jwtToken = env.getProperty("rsocket.jwt-token", "").toCharArray();
        this.socketAcceptor = socketAcceptor;
    }

    @Override
    public URI originUri() {
        return URI.create(properties.getSchema() + "://" + NetworkUtil.LOCAL_IP + ":" + properties.getPort()
                + "?appName=" + appName
                + "&uuid=" + RSocketAppContext.ID);
    }

    @Override
    public Supplier<Payload> setupPayload() {
        return () -> {
            //composite metadata with app metadata
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(getAppMetadata());
            //add published in setup payload
            Set<ServiceLocator> serviceLocators = exposedServices().get();
            if (!compositeMetadata.contains(RSocketMimeType.ServiceRegistry) && !serviceLocators.isEmpty()) {
                ServiceRegistryMetadata serviceRegistryMetadata = new ServiceRegistryMetadata();
                serviceRegistryMetadata.setPublished(serviceLocators);
                compositeMetadata.addMetadata(serviceRegistryMetadata);
            }
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
            return applicationContext.getBeansWithAnnotation(RSocketService.class)
                    .values()
                    .stream()
                    .filter(bean -> !(bean instanceof RSocketServiceHealth || bean instanceof MetricsService))
                    .map(o -> {
                        Class<?> managedBeanClass = AopUtils.isAopProxy(o) ? AopUtils.getTargetClass(o) : o.getClass();
                        RSocketService rSocketService = AnnotationUtils.findAnnotation(managedBeanClass, RSocketService.class);
                        //noinspection ConstantConditions
                        String serviceName = rSocketService.serviceInterface().getCanonicalName();
                        if (!rSocketService.name().isEmpty()) {
                            serviceName = rSocketService.name();
                        }
                        return new ServiceLocator(
                                properties.getGroup(),
                                serviceName,
                                properties.getVersion(),
                                rSocketService.tags()
                        );
                    }).collect(Collectors.toSet());
        };
    }

    @Override
    public Supplier<Set<ServiceLocator>> subscribedServices() {
        return () -> RSocketRemoteServiceBuilder.CONSUMED_SERVICES;
    }

    @Override
    public Supplier<CloudEventImpl<ServicesExposedEvent>> servicesExposedEvent() {
        return () -> {
            Collection<ServiceLocator> serviceLocators = exposedServices().get();
            if (serviceLocators.isEmpty()) return null;
            return ServicesExposedEvent.convertServicesToCloudEvent(serviceLocators);
        };
    }

    @NotNull
    private AppMetadata getAppMetadata() {
        //app metadata
        AppMetadata appMetadata = new AppMetadata();
        appMetadata.setUuid(RSocketAppContext.ID);
        appMetadata.setName(appName);
        appMetadata.setIp(NetworkUtil.LOCAL_IP);
        appMetadata.setDevice("SpringBootApp");
        appMetadata.setRsocketPorts(RSocketAppContext.rsocketPorts);
        //brokers
        appMetadata.setBrokers(properties.getBrokers());
        appMetadata.setTopology(properties.getTopology());
        //web port
        appMetadata.setWebPort(Integer.parseInt(env.getProperty("server.port", "0")));
        appMetadata.setManagementPort(appMetadata.getWebPort());
        //management port
        if (env.getProperty("management.server.port") != null) {
            appMetadata.setManagementPort(Integer.parseInt(env.getProperty("management.server.port")));
        }
        if (appMetadata.getWebPort() <= 0) {
            appMetadata.setWebPort(RSocketAppContext.webPort);
        }
        if (appMetadata.getManagementPort() <= 0) {
            appMetadata.setManagementPort(RSocketAppContext.managementPort);
        }
        //labels
        appMetadata.setMetadata(new HashMap<>());
        env.stringPropertyNames().forEach(key -> {
            if (key.startsWith("rsocket.metadata.")) {
                String[] parts = key.split("[=:]", 2);
                appMetadata.getMetadata().put(parts[0].trim().replace("rsocket.metadata.", ""), env.getProperty(key));
            }
        });
        //power unit
        if (appMetadata.getMetadata("power-rating") != null) {
            appMetadata.setPowerRating(Integer.parseInt(appMetadata.getMetadata("power-rating")));
        }
        //humans.md
        URL humansMd = this.getClass().getResource("/humans.md");
        if (humansMd != null) {
            try (InputStream inputStream = humansMd.openStream()) {
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                inputStream.close();
                appMetadata.setHumansMd(new String(bytes, StandardCharsets.UTF_8));
            } catch (Exception ignore) {

            }
        }
        return appMetadata;
    }

    @Override
    public SocketAcceptor socketAcceptor() {
        return this.socketAcceptor;
    }

    @Override
    public List<RSocketInterceptor> responderInterceptors() {
        return this.responderInterceptors;
    }

    public void addResponderInterceptor(RSocketInterceptor interceptor) {
        this.responderInterceptors.add(interceptor);
    }

    @Override
    public List<RSocketInterceptor> requestInterceptors() {
        return requestInterceptors;
    }

    public void addRequesterInterceptor(RSocketInterceptor interceptor) {
        this.requestInterceptors.add(interceptor);
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
