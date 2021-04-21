package com.alibaba.spring.boot.rsocket;

import brave.Tracing;
import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.events.CloudEventsConsumer;
import com.alibaba.rsocket.events.CloudEventsProcessor;
import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.rsocket.listen.RSocketResponderHandlerFactory;
import com.alibaba.rsocket.observability.MetricsService;
import com.alibaba.rsocket.route.RoutingEndpoint;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.rpc.RSocketResponderHandler;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamClusterChangedEventConsumer;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.spring.boot.rsocket.health.RSocketServiceHealthImpl;
import com.alibaba.spring.boot.rsocket.observability.MetricsServicePrometheusImpl;
import com.alibaba.spring.boot.rsocket.responder.RSocketServicesPublishHook;
import com.alibaba.spring.boot.rsocket.responder.invocation.RSocketServiceAnnotationProcessor;
import com.alibaba.spring.boot.rsocket.upstream.JwtTokenNotFoundException;
import com.alibaba.spring.boot.rsocket.upstream.RSocketRequesterSupportBuilderImpl;
import com.alibaba.spring.boot.rsocket.upstream.RSocketRequesterSupportCustomizer;
import com.alibaba.spring.boot.rsocket.upstream.SmartLifecycleUpstreamManagerImpl;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.rsocket.SocketAcceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;
import reactor.extra.processor.TopicProcessor;

import java.util.stream.Collectors;


/**
 * RSocket Auto configuration: listen, upstream manager, handler etc
 *
 * @author leijuan
 */
@SuppressWarnings({"rawtypes", "SpringJavaInjectionPointsAutowiringInspection"})
@Configuration
@ConditionalOnExpression("${rsocket.disabled:false}==false")
@EnableConfigurationProperties(RSocketProperties.class)
public class RSocketAutoConfiguration {
    @Autowired
    private RSocketProperties properties;
    @Value("${server.port:0}")
    private int serverPort;
    @Value("${management.server.port:0}")
    private int managementServerPort;
    @Autowired
    private ApplicationContext applicationContext;

    // section cloudevents processor
    @Bean
    public TopicProcessor<CloudEventImpl> reactiveCloudEventProcessor() {
        return TopicProcessor.<CloudEventImpl>builder().name("cloud-events-processor").build();
    }

    @Bean(initMethod = "init")
    public CloudEventsProcessor cloudEventsProcessor(@Autowired @Qualifier("reactiveCloudEventProcessor") TopicProcessor<CloudEventImpl> eventProcessor,
                                                     ObjectProvider<CloudEventsConsumer> consumers) {
        return new CloudEventsProcessor(eventProcessor, consumers.stream().collect(Collectors.toList()));
    }

    @Bean
    public UpstreamClusterChangedEventConsumer upstreamClusterChangedEventConsumer(@Autowired UpstreamManager upstreamManager) {
        return new UpstreamClusterChangedEventConsumer(upstreamManager);
    }

    @Bean
    public CloudEventToListenerConsumer cloudEventToListenerConsumer() {
        return new CloudEventToListenerConsumer();
    }

  /*  @Bean
    public InvalidCacheEventConsumer invalidCacheEventConsumer() {
        return new InvalidCacheEventConsumer();
    }*/

    /**
     * socket responder handler as SocketAcceptor bean.
     * To validate connection, please use RSocketListenerCustomizer and add AcceptorInterceptor by addSocketAcceptorInterceptor api
     *
     * @param serviceCaller  service caller
     * @param eventProcessor event processor
     * @return handler factor
     */
    @Bean
    @ConditionalOnMissingBean(type = {"brave.Tracing", "com.alibaba.rsocket.listen.RSocketResponderHandlerFactory"})
    public RSocketResponderHandlerFactory rsocketResponderHandlerFactory(@Autowired LocalReactiveServiceCaller serviceCaller,
                                                                         @Autowired @Qualifier("reactiveCloudEventProcessor") TopicProcessor<CloudEventImpl> eventProcessor) {
        return (setupPayload, requester) -> Mono.fromCallable(() -> new RSocketResponderHandler(serviceCaller, eventProcessor, requester, setupPayload));
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(type = "brave.Tracing")
    public RSocketResponderHandlerFactory rsocketResponderHandlerFactoryWithZipkin(@Autowired LocalReactiveServiceCaller serviceCaller,
                                                                                   @Autowired @Qualifier("reactiveCloudEventProcessor") TopicProcessor<CloudEventImpl> eventProcessor) {
        return (setupPayload, requester) -> Mono.fromCallable(() -> {
            RSocketResponderHandler responderHandler = new RSocketResponderHandler(serviceCaller, eventProcessor, requester, setupPayload);
            Tracing tracing = applicationContext.getBean(Tracing.class);
            responderHandler.setTracer(tracing.tracer());
            return responderHandler;
        });
    }

    @Bean
    @ConditionalOnMissingBean(RSocketRequesterSupport.class)
    public RSocketRequesterSupport rsocketRequesterSupport(@Autowired RSocketProperties properties,
                                                           @Autowired Environment environment,
                                                           @Autowired SocketAcceptor socketAcceptor,
                                                           @Autowired ObjectProvider<RSocketRequesterSupportCustomizer> customizers) {
        RSocketRequesterSupportBuilderImpl builder = new RSocketRequesterSupportBuilderImpl(properties, new EnvironmentProperties(environment), socketAcceptor);
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(LocalReactiveServiceCaller.class)
    public RSocketServiceAnnotationProcessor rSocketServiceAnnotationProcessor(RSocketProperties rsocketProperties) {
        return new RSocketServiceAnnotationProcessor(rsocketProperties);
    }

    @Bean(initMethod = "init")
    public UpstreamManager rsocketUpstreamManager(@Autowired RSocketRequesterSupport rsocketRequesterSupport) throws JwtTokenNotFoundException {
        UpstreamManager upstreamManager = new SmartLifecycleUpstreamManagerImpl(rsocketRequesterSupport);
        if (properties.getBrokers() != null && !properties.getBrokers().isEmpty()) {
            if (properties.getJwtToken() == null || properties.getJwtToken().isEmpty()) {
                throw new JwtTokenNotFoundException();
            }
            UpstreamCluster cluster = new UpstreamCluster(null, "*", null);
            cluster.setUris(properties.getBrokers());
            upstreamManager.add(cluster);
        }
        if (properties.getRoutes() != null && !properties.getRoutes().isEmpty()) {
            for (RoutingEndpoint route : properties.getRoutes()) {
                UpstreamCluster cluster = new UpstreamCluster(route.getGroup(), route.getService(), route.getVersion());
                cluster.setUris(route.getUris());
                upstreamManager.add(cluster);
            }
        }
        return upstreamManager;
    }

    @Bean
    @ConditionalOnProperty("rsocket.brokers")
    public RSocketBrokerHealthIndicator rsocketBrokerHealth(RSocketEndpoint rsocketEndpoint, UpstreamManager upstreamManager, @Value("${rsocket.brokers}") String brokers) {
        return new RSocketBrokerHealthIndicator(rsocketEndpoint, upstreamManager, brokers);
    }

    @Bean
    public RSocketEndpoint rsocketEndpoint(@Autowired UpstreamManager upstreamManager, @Autowired RSocketRequesterSupport rsocketRequesterSupport) {
        return new RSocketEndpoint(properties, upstreamManager, rsocketRequesterSupport);
    }

    @Bean
    @ConditionalOnClass(PrometheusMeterRegistry.class)
    public MetricsService metricsService(PrometheusMeterRegistry meterRegistry) {
        return new MetricsServicePrometheusImpl(meterRegistry);
    }

    @Bean
    public RSocketServicesPublishHook rsocketServicesPublishHook() {
        return new RSocketServicesPublishHook();
    }

    @Bean
    @ConditionalOnMissingBean
    public RSocketServiceHealth rsocketServiceHealth() {
        return new RSocketServiceHealthImpl();
    }

    @Bean
    public ApplicationListener<WebServerInitializedEvent> webServerInitializedEventApplicationListener() {
        return webServerInitializedEvent -> {
            String namespace = webServerInitializedEvent.getApplicationContext().getServerNamespace();
            int listenPort = webServerInitializedEvent.getWebServer().getPort();
            if ("management".equals(namespace)) {
                this.managementServerPort = listenPort;
                RSocketAppContext.managementPort = listenPort;
            } else {
                this.serverPort = listenPort;
                RSocketAppContext.webPort = listenPort;
                if (this.managementServerPort == 0) {
                    this.managementServerPort = listenPort;
                    RSocketAppContext.managementPort = listenPort;
                }
            }
        };
    }
}
