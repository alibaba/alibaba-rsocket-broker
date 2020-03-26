package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.rsocket.listen.RSocketResponderHandlerFactory;
import com.alibaba.rsocket.observability.MetricsService;
import com.alibaba.rsocket.route.RoutingEndpoint;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.rpc.RSocketResponderHandler;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.rsocket.upstream.UpstreamManagerImpl;
import com.alibaba.spring.boot.rsocket.health.RSocketServiceHealthImpl;
import com.alibaba.spring.boot.rsocket.observability.MetricsServicePrometheusImpl;
import com.alibaba.spring.boot.rsocket.responder.RSocketServicesPublishHook;
import com.alibaba.spring.boot.rsocket.responder.invocation.RSocketServiceAnnotationProcessor;
import com.alibaba.spring.boot.rsocket.upstream.JwtTokenNotFoundException;
import com.alibaba.spring.boot.rsocket.upstream.RSocketRequesterSupportBuilderImpl;
import com.alibaba.spring.boot.rsocket.upstream.RSocketRequesterSupportCustomizer;
import io.cloudevents.v1.CloudEventImpl;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.rsocket.SocketAcceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;
import reactor.extra.processor.TopicProcessor;

/**
 * RSocket Auto configuration: listen, upstream manager, handler etc
 *
 * @author leijuan
 */
@SuppressWarnings("deprecation")
@Configuration
@EnableConfigurationProperties(RSocketProperties.class)
public class RSocketAutoConfiguration {
    @Autowired
    private RSocketProperties properties;

    @Bean
    public TopicProcessor<CloudEventImpl> reactiveCloudEventProcessor() {
        return TopicProcessor.<CloudEventImpl>builder().name("cloud-events-processor").build();
    }

    @Bean(initMethod = "init")
    public RequesterCloudEventProcessor requesterCloudEventProcessor() {
        return new RequesterCloudEventProcessor();
    }

    /**
     * socket responder handler as SocketAcceptor bean.
     * To validate connection, please use RSocketListenerCustomizer and add AcceptorInterceptor by addSocketAcceptorInterceptor api
     *
     * @param serviceCaller  service caller
     * @param eventProcessor event processor
     * @return handler factor
     */
    @Bean
    @ConditionalOnMissingBean
    public RSocketResponderHandlerFactory rsocketResponderHandlerFactory(@Autowired LocalReactiveServiceCaller serviceCaller,
                                                                         @Autowired @Qualifier("reactiveCloudEventProcessor") TopicProcessor<CloudEventImpl> eventProcessor) {
        return (setupPayload, requester) -> Mono.fromCallable(() -> new RSocketResponderHandler(serviceCaller, eventProcessor, requester, setupPayload));
    }

    @Bean
    public RSocketRequesterSupport rsocketRequesterSupport(@Autowired RSocketProperties properties,
                                                           @Autowired Environment environment,
                                                           @Autowired SocketAcceptor socketAcceptor,
                                                           @Autowired ObjectProvider<RSocketRequesterSupportCustomizer> customizers) {
        RSocketRequesterSupportBuilderImpl builder = new RSocketRequesterSupportBuilderImpl(properties, new EnvironmentProperties(environment), socketAcceptor);
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    public RSocketServiceAnnotationProcessor rSocketServiceAnnotationProcessor(RSocketProperties rsocketProperties) {
        return new RSocketServiceAnnotationProcessor(rsocketProperties);
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    public UpstreamManager rsocketUpstreamManager(@Autowired RSocketRequesterSupport rsocketRequesterSupport) throws JwtTokenNotFoundException {
        UpstreamManager upstreamManager = new UpstreamManagerImpl(rsocketRequesterSupport);
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
}
