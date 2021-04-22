package com.alibaba.spring.boot.rsocket.broker;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.discovery.DiscoveryService;
import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.rsocket.listen.RSocketListener;
import com.alibaba.rsocket.listen.RSocketListenerCustomizer;
import com.alibaba.rsocket.route.RSocketFilter;
import com.alibaba.rsocket.route.RSocketFilterChain;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.spring.boot.rsocket.broker.cluster.DefaultRSocketBrokerManager;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManagerDiscoveryImpl;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManagerGossipImpl;
import com.alibaba.spring.boot.rsocket.broker.impl.BrokerRSocketServiceHealthImpl;
import com.alibaba.spring.boot.rsocket.broker.impl.DiscoveryServiceImpl;
import com.alibaba.spring.boot.rsocket.broker.responder.AppStatusCloudEventProcessor;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistryImpl;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceMeshInspector;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.alibaba.spring.boot.rsocket.broker.route.impl.ServiceMeshInspectorImpl;
import com.alibaba.spring.boot.rsocket.broker.route.impl.ServiceRoutingSelectorImpl;
import com.alibaba.spring.boot.rsocket.broker.security.AuthenticationService;
import com.alibaba.spring.boot.rsocket.broker.security.AuthenticationServiceJwtImpl;
import com.alibaba.spring.boot.rsocket.broker.services.*;
import com.alibaba.spring.boot.rsocket.broker.smi.TrafficAccessControl;
import com.alibaba.spring.boot.rsocket.broker.smi.TrafficSplit;
import com.alibaba.spring.boot.rsocket.broker.smi.impl.TrafficAccessControlImpl;
import com.alibaba.spring.boot.rsocket.broker.smi.impl.TrafficSplitImpl;
import com.alibaba.spring.boot.rsocket.broker.supporting.RSocketLocalServiceAnnotationProcessor;
import com.alibaba.spring.boot.rsocket.broker.upstream.RSocketRequesterBySubBroker;
import com.alibaba.spring.boot.rsocket.broker.upstream.UpstreamBrokerCluster;
import io.rsocket.RSocket;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import reactor.core.publisher.Sinks;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.stream.Collectors;


/**
 * RSocket broker auto configuration
 *
 * @author leijuan
 */
@SuppressWarnings("rawtypes")
@Configuration
@EnableConfigurationProperties(RSocketBrokerProperties.class)
public class RSocketBrokerAutoConfiguration {
    @Bean
    public RSocketFilterChain rsocketFilterChain(ObjectProvider<RSocketFilter> rsocketFilters) {
        return new RSocketFilterChain(rsocketFilters.orderedStream().collect(Collectors.toList()));
    }

    @Bean
    public ServiceMeshInspector serviceMeshInspector(RSocketBrokerProperties brokerProperties) {
        return new ServiceMeshInspectorImpl(brokerProperties.isAuthRequired());
    }

    @Bean
    public ServiceRoutingSelector serviceRoutingSelector() {
        return new ServiceRoutingSelectorImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigurationService configurationService() {
        return new KVStorageServiceImpl();
    }

    @Bean
    public ConfigController configController() {
        return new ConfigController();
    }

    @Bean
    public AppQueryController appQueryController() {
        return new AppQueryController();
    }

    @Bean
    public ServiceQueryController serviceQueryController() {
        return new ServiceQueryController();
    }

    @Bean
    public MetricsScrapeController metricsScrapeController() {
        return new MetricsScrapeController();
    }

    @Bean
    public RSocketBrokerHandlerRegistry rsocketResponderHandlerRegistry(@Autowired LocalReactiveServiceCaller localReactiveServiceCaller,
                                                                        @Autowired RSocketFilterChain rsocketFilterChain,
                                                                        @Autowired ServiceRoutingSelector routingSelector,
                                                                        @Autowired @Qualifier("reactiveCloudEventProcessor") Sinks.Many<CloudEventImpl> eventProcessor,
                                                                        @Autowired @Qualifier("notificationProcessor") Sinks.Many<String> notificationProcessor,
                                                                        @Autowired AuthenticationService authenticationService,
                                                                        @Autowired RSocketBrokerManager rSocketBrokerManager,
                                                                        @Autowired ServiceMeshInspector serviceMeshInspector,
                                                                        @Autowired RSocketBrokerProperties properties,
                                                                        @Autowired ApplicationContext applicationContext) {
        return new RSocketBrokerHandlerRegistryImpl(localReactiveServiceCaller, rsocketFilterChain, routingSelector,
                eventProcessor, notificationProcessor, authenticationService, rSocketBrokerManager, serviceMeshInspector,
                properties.isAuthRequired(), applicationContext);
    }

    @Bean
    public RSocketLocalServiceAnnotationProcessor rSocketServiceAnnotationProcessor() {
        return new RSocketLocalServiceAnnotationProcessor();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public RSocketListener rsocketListener(ObjectProvider<RSocketListenerCustomizer> customizers) {
        RSocketListener.Builder builder = RSocketListener.builder();
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    @Order(100)
    public RSocketListenerCustomizer defaultRSocketListenerCustomizer(@Autowired RSocketBrokerHandlerRegistry registry,
                                                                      @Autowired RSocketBrokerProperties properties) {
        return builder -> {
            builder.acceptor(registry);
            builder.listen("tcp", properties.getListen());
        };
    }

    @Bean
    @Order(101)
    @ConditionalOnProperty(name = "rsocket.broker.ssl.key-store")
    RSocketListenerCustomizer rSocketListenerSSLCustomizer(@Autowired RSocketBrokerProperties properties,
                                                           @Autowired ResourceLoader resourceLoader) {
        return builder -> {
            RSocketBrokerProperties.RSocketSSL rSocketSSL = properties.getSsl();
            if (rSocketSSL != null && rSocketSSL.isEnabled() && rSocketSSL.getKeyStore() != null) {
                try {
                    KeyStore store = KeyStore.getInstance("PKCS12");
                    store.load(resourceLoader.getResource(rSocketSSL.getKeyStore()).getInputStream(), rSocketSSL.getKeyStorePassword().toCharArray());
                    String alias = store.aliases().nextElement();
                    Certificate certificate = store.getCertificate(alias);
                    KeyStore.Entry entry = store.getEntry(alias, new KeyStore.PasswordProtection(rSocketSSL.getKeyStorePassword().toCharArray()));
                    PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
                    builder.sslContext(certificate, privateKey);
                    builder.listen("tcps", properties.getListen());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Bean
    public DiscoveryService rsocketDiscoveryService() {
        return new DiscoveryServiceImpl();
    }

    @Bean
    public AuthenticationService authenticationService() throws Exception {
        return new AuthenticationServiceJwtImpl();
    }

    @Bean
    public RSocketServiceHealth brokRSocketServiceHealth(@Autowired ServiceRoutingSelector routingSelector) {
        return new BrokerRSocketServiceHealthImpl(routingSelector);
    }

    @Bean
    @ConditionalOnMissingBean(RSocketBrokerManager.class)
    public RSocketBrokerManager rsocketBrokerManager() {
        return new DefaultRSocketBrokerManager();
    }

    @Bean
    @ConditionalOnExpression("'${rsocket.broker.topology}'=='gossip'")
    @Primary
    public RSocketBrokerManager rsocketGossipBrokerManager() {
        return new RSocketBrokerManagerGossipImpl();
    }

    @Bean
    @ConditionalOnExpression("'${rsocket.broker.topology}'=='discovery' || '${rsocket.broker.topology}'=='k8s'")
    @Primary
    public RSocketBrokerManager rsocketDiscoveryBrokerManager(ReactiveDiscoveryClient discoveryClient) {
        return new RSocketBrokerManagerDiscoveryImpl(discoveryClient);
    }

    @Bean
    public Sinks.Many<CloudEventImpl> reactiveCloudEventProcessor() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<String> notificationProcessor() {
        return Sinks.many().multicast().onBackpressureBuffer(8);
    }

    @Bean(initMethod = "init")
    public AppStatusCloudEventProcessor appStatusCloudEventProcessor() {
        return new AppStatusCloudEventProcessor();
    }

    @Bean
    public TrafficAccessControl trafficAccessControl() {
        return new TrafficAccessControlImpl();
    }

    @Bean
    public TrafficSplit trafficSplit() {
        return new TrafficSplitImpl();
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    @ConditionalOnProperty(name = "rsocket.broker.upstream-brokers")
    public UpstreamBrokerCluster upstreamBrokerCluster(@Autowired RSocketBrokerProperties properties,
                                                       @Autowired Environment env,
                                                       @Autowired RSocketFilterChain filterChain,
                                                       @Autowired ServiceRoutingSelector serviceRoutingSelector,
                                                       @Autowired RSocketBrokerHandlerRegistry rsocketResponderHandlerRegistry) {
        RSocketRequesterBySubBroker subBroker = new RSocketRequesterBySubBroker(properties, env, rsocketResponderHandlerRegistry, filterChain, serviceRoutingSelector);
        return new UpstreamBrokerCluster(subBroker);
    }

    @Bean
    @Qualifier("upstreamBrokerRSocket")
    @ConditionalOnProperty(name = "rsocket.broker.upstream-brokers")
    public RSocket upstreamBrokerRSocket(UpstreamBrokerCluster upstreamBrokerCluster) {
        return upstreamBrokerCluster.getLoadBalancedRSocket();
    }

}
