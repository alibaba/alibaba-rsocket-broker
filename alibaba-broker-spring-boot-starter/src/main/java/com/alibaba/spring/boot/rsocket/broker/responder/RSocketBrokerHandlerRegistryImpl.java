package com.alibaba.spring.boot.rsocket.broker.responder;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.rsocket.metadata.BearerTokenMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.route.RSocketFilterChain;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent;
import com.alibaba.rsocket.utils.MurmurHash3;
import com.alibaba.spring.boot.rsocket.broker.BrokerAppContext;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBroker;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceMeshInspector;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.alibaba.spring.boot.rsocket.broker.security.AuthenticationService;
import com.alibaba.spring.boot.rsocket.broker.security.JwtPrincipal;
import com.alibaba.spring.boot.rsocket.broker.security.RSocketAppPrincipal;
import io.micrometer.core.instrument.Metrics;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.exceptions.ApplicationErrorException;
import io.rsocket.exceptions.RejectedSetupException;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RSocket broker handler registry implementation
 *
 * @author leijuan
 */
@SuppressWarnings("rawtypes")
public class RSocketBrokerHandlerRegistryImpl implements RSocketBrokerHandlerRegistry {
    private final Logger log = LoggerFactory.getLogger(RSocketBrokerHandlerRegistryImpl.class);
    private RSocketFilterChain rsocketFilterChain;
    private LocalReactiveServiceCaller localReactiveServiceCaller;
    private ServiceRoutingSelector routingSelector;
    private Sinks.Many<CloudEventImpl> eventProcessor;
    private Sinks.Many<String> notificationProcessor;
    private AuthenticationService authenticationService;
    /**
     * connections, key is connection id
     */
    private Map<Integer, RSocketBrokerResponderHandler> connectionHandlers = new ConcurrentHashMap<>();
    /**
     * broker side handlers, the key is app instance UUID
     */
    private Map<String, RSocketBrokerResponderHandler> responderHandlers = new ConcurrentHashMap<>();
    /**
     * handlers for App name, the key is app name, and value is list of handlers
     */
    private FastListMultimap<String, RSocketBrokerResponderHandler> appHandlers = new FastListMultimap<>();
    private RSocketBrokerManager rsocketBrokerManager;
    private ServiceMeshInspector serviceMeshInspector;
    private boolean authRequired;
    private ApplicationContext applicationContext;

    public RSocketBrokerHandlerRegistryImpl(LocalReactiveServiceCaller localReactiveServiceCaller, RSocketFilterChain rsocketFilterChain,
                                            ServiceRoutingSelector routingSelector,
                                            Sinks.Many<CloudEventImpl> eventProcessor,
                                            Sinks.Many<String> notificationProcessor,
                                            AuthenticationService authenticationService,
                                            RSocketBrokerManager rsocketBrokerManager,
                                            ServiceMeshInspector serviceMeshInspector,
                                            boolean authRequired,
                                            ApplicationContext applicationContext) {
        this.localReactiveServiceCaller = localReactiveServiceCaller;
        this.rsocketFilterChain = rsocketFilterChain;
        this.routingSelector = routingSelector;
        this.eventProcessor = eventProcessor;
        this.notificationProcessor = notificationProcessor;
        this.authenticationService = authenticationService;
        this.rsocketBrokerManager = rsocketBrokerManager;
        this.serviceMeshInspector = serviceMeshInspector;
        this.authRequired = authRequired;
        this.applicationContext = applicationContext;
        if (!rsocketBrokerManager.isStandAlone()) {
            this.rsocketBrokerManager.requestAll().flatMap(this::broadcastClusterTopology).subscribe();
        }
        //gauge metrics
        Metrics.globalRegistry.gauge("broker.apps.count", this, (DoubleFunction<RSocketBrokerHandlerRegistryImpl>) handlerRegistry -> handlerRegistry.appHandlers.size());
        Metrics.globalRegistry.gauge("broker.service.provider.count", this, (DoubleFunction<RSocketBrokerHandlerRegistryImpl>) handlerRegistry -> handlerRegistry.appHandlers.valuesView().sumOfInt(handler -> handler.getPeerServices() == null ? 0 : 1));
        Metrics.globalRegistry.gauge("broker.service.count", this.routingSelector, (DoubleFunction<ServiceRoutingSelector>) ServiceRoutingSelector::getDistinctServiceCount);
    }

    @Override
    @Nullable
    public Mono<RSocket> accept(final ConnectionSetupPayload setupPayload, final RSocket requesterSocket) {
        //parse setup payload
        RSocketCompositeMetadata compositeMetadata = null;
        AppMetadata appMetadata = null;
        String credentials = "";
        RSocketAppPrincipal principal = null;
        String errorMsg = null;
        try {
            compositeMetadata = RSocketCompositeMetadata.from(setupPayload.metadata());
            if (!authRequired) {  //authentication not required
                principal = appNameBasedPrincipal("MockApp");
                credentials = UUID.randomUUID().toString();
            } else if (compositeMetadata.contains(RSocketMimeType.BearerToken)) {
                BearerTokenMetadata bearerTokenMetadata = BearerTokenMetadata.from(compositeMetadata.getMetadata(RSocketMimeType.BearerToken));
                credentials = new String(bearerTokenMetadata.getBearerToken());
                principal = authenticationService.auth("JWT", credentials);
            } else { // no jwt token supplied
                errorMsg = RsocketErrorCode.message("RST-500405");
            }
            //validate application information
            if (principal != null && compositeMetadata.contains(RSocketMimeType.Application)) {
                AppMetadata temp = AppMetadata.from(compositeMetadata.getMetadata(RSocketMimeType.Application));
                //App registration validation: app id: UUID and unique in server
                if (temp.getUuid() == null || temp.getUuid().isEmpty()) {
                    temp.setUuid(UUID.randomUUID().toString());
                }
                String appId = temp.getUuid();
                //validate appId data format
                if (appId != null && appId.length() >= 32) {
                    Integer instanceId = MurmurHash3.hash32(credentials + ":" + temp.getUuid());
                    temp.setId(instanceId);
                    //application instance not connected
                    if (!routingSelector.containInstance(instanceId)) {
                        appMetadata = temp;
                        appMetadata.setConnectedAt(new Date());
                    } else {  // application connected already
                        errorMsg = RsocketErrorCode.message("RST-500409");
                    }
                } else {  //illegal application id, appID should be UUID
                    errorMsg = RsocketErrorCode.message("RST-500410", appId == null ? "" : appId);
                }
            }
            if (errorMsg == null) {
                //Security authentication
                if (appMetadata != null) {
                    appMetadata.addMetadata("_orgs", String.join(",", principal.getOrganizations()));
                    appMetadata.addMetadata("_roles", String.join(",", principal.getRoles()));
                    appMetadata.addMetadata("_serviceAccounts", String.join(",", principal.getServiceAccounts()));
                } else {
                    errorMsg = RsocketErrorCode.message("RST-500411");
                }
            }
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-500402"), e);
            errorMsg = RsocketErrorCode.message("RST-600500", e.getMessage());
        }
        //validate connection legal or not
        if (principal == null) {
            errorMsg = RsocketErrorCode.message("RST-500405");
        }
        if (errorMsg != null) {
            return returnRejectedRSocket(errorMsg, requesterSocket);
        }
        //create handler
        try {
            RSocketBrokerResponderHandler brokerResponderHandler = new RSocketBrokerResponderHandler(setupPayload, compositeMetadata, appMetadata, principal,
                    requesterSocket, routingSelector, eventProcessor, this, serviceMeshInspector, getUpstreamRSocket());
            brokerResponderHandler.setFilterChain(rsocketFilterChain);
            brokerResponderHandler.setLocalReactiveServiceCaller(localReactiveServiceCaller);
            brokerResponderHandler.onClose()
                    .doOnTerminate(() -> onHandlerDisposed(brokerResponderHandler))
                    .subscribeOn(Schedulers.parallel()).subscribe();
            //handler registration notify
            onHandlerRegistered(brokerResponderHandler);
            log.info(RsocketErrorCode.message("RST-500200", appMetadata.getName()));
            return Mono.just(brokerResponderHandler);
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-500406", e.getMessage()), e);
            return returnRejectedRSocket(RsocketErrorCode.message("RST-500406", e.getMessage()), requesterSocket);
        }
    }

    @Override
    public Collection<String> findAllAppNames() {
        return appHandlers.keySet().toList();
    }

    @Override
    public Collection<RSocketBrokerResponderHandler> findAll() {
        return responderHandlers.values();
    }

    @Override
    public Collection<RSocketBrokerResponderHandler> findByAppName(String appName) {
        return appHandlers.get(appName);
    }

    @Override
    public RSocketBrokerResponderHandler findByUUID(String id) {
        return responderHandlers.get(id);
    }

    @Override
    public @Nullable RSocketBrokerResponderHandler findById(Integer id) {
        return connectionHandlers.get(id);
    }

    @Override
    public void onHandlerRegistered(RSocketBrokerResponderHandler responderHandler) {
        AppMetadata appMetadata = responderHandler.getAppMetadata();
        responderHandlers.put(appMetadata.getUuid(), responderHandler);
        connectionHandlers.put(responderHandler.getId(), responderHandler);
        appHandlers.put(appMetadata.getName(), responderHandler);
        eventProcessor.tryEmitNext(appStatusEventCloudEvent(appMetadata, AppStatusEvent.STATUS_CONNECTED));
        if (!rsocketBrokerManager.isStandAlone()) {
            responderHandler.fireCloudEventToPeer(getBrokerClustersEvent(rsocketBrokerManager.currentBrokers(), appMetadata.getTopology())).subscribe();
        }
        this.notificationProcessor.tryEmitNext(RsocketErrorCode.message("RST-300203", appMetadata.getName(), appMetadata.getIp()));
    }

    @Override
    public void onHandlerDisposed(RSocketBrokerResponderHandler responderHandler) {
        AppMetadata appMetadata = responderHandler.getAppMetadata();
        responderHandlers.remove(responderHandler.getUuid());
        connectionHandlers.remove(responderHandler.getId());
        appHandlers.remove(appMetadata.getName(), responderHandler);
        log.info(RsocketErrorCode.message("RST-500202"));
        responderHandler.clean();
        eventProcessor.tryEmitNext(appStatusEventCloudEvent(appMetadata, AppStatusEvent.STATUS_STOPPED));
        this.notificationProcessor.tryEmitNext(RsocketErrorCode.message("RST-300204", appMetadata.getName(), appMetadata.getIp()));
    }

    @Override
    public Multimap<String, RSocketBrokerResponderHandler> appHandlers() {
        return appHandlers;
    }

    @Override
    public Mono<Void> broadcast(@NotNull String appName, final CloudEventImpl cloudEvent) {
        if (appName.equals("*")) {
            return Flux.fromIterable(connectionHandlers.values())
                    .flatMap(handler -> handler.fireCloudEventToPeer(cloudEvent))
                    .then();
        } else if (appHandlers.containsKey(appName)) {
            return Flux.fromIterable(appHandlers.get(appName))
                    .flatMap(handler -> handler.fireCloudEventToPeer(cloudEvent))
                    .then();
        } else {
            return Mono.error(new ApplicationErrorException("Application not found:" + appName));
        }
    }

    @Override
    public Mono<Void> broadcastAll(CloudEventImpl cloudEvent) {
        return Flux.fromIterable(appHandlers.keySet())
                .flatMap(name -> Flux.fromIterable(appHandlers.get(name)))
                .flatMap(handler -> handler.fireCloudEventToPeer(cloudEvent))
                .then();
    }

    @Override
    public Mono<Void> send(@NotNull String appUUID, CloudEventImpl cloudEvent) {
        RSocketBrokerResponderHandler responderHandler = responderHandlers.get(appUUID);
        if (responderHandler != null) {
            return responderHandler.fireCloudEvent(cloudEvent);
        } else {
            return Mono.error(new ApplicationErrorException("Application not found:" + appUUID));
        }
    }

    public void cleanStaleHandlers() {
        //todo clean stale handlers
        /*Flux.interval(Duration.ofSeconds(10)).subscribe(t -> {

        });*/
    }

    private CloudEventImpl<AppStatusEvent> appStatusEventCloudEvent(AppMetadata appMetadata, Integer status) {
        return RSocketCloudEventBuilder
                .builder(new AppStatusEvent(appMetadata.getUuid(), status))
                .build();
    }

    private CloudEventImpl<UpstreamClusterChangedEvent> getBrokerClustersEvent(Collection<RSocketBroker> rSocketBrokers, String topology) {
        List<String> uris;
        if ("internet".equals(topology)) {
            uris = rSocketBrokers.stream()
                    .filter(rsocketBroker -> rsocketBroker.isActive() && rsocketBroker.getExternalDomain() != null)
                    .map(RSocketBroker::getAliasUrl)
                    .collect(Collectors.toList());
        } else {
            uris = rSocketBrokers.stream()
                    .filter(RSocketBroker::isActive)
                    .map(RSocketBroker::getUrl)
                    .collect(Collectors.toList());
        }
        UpstreamClusterChangedEvent upstreamClusterChangedEvent = new UpstreamClusterChangedEvent();
        upstreamClusterChangedEvent.setGroup("");
        upstreamClusterChangedEvent.setInterfaceName("*");
        upstreamClusterChangedEvent.setVersion("");
        upstreamClusterChangedEvent.setUris(uris);

        // passing in the given attributes
        return RSocketCloudEventBuilder.builder(upstreamClusterChangedEvent)
                .withDataschema(URI.create("rsocket:event:com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent"))
                .withSource(BrokerAppContext.identity())
                .build();
    }

    private Flux<Void> broadcastClusterTopology(Collection<RSocketBroker> rSocketBrokers) {
        final CloudEventImpl<UpstreamClusterChangedEvent> brokerClustersEvent = getBrokerClustersEvent(rSocketBrokers, "intranet");
        final CloudEventImpl<UpstreamClusterChangedEvent> brokerClusterAliasesEvent = getBrokerClustersEvent(rSocketBrokers, "internet");
        return Flux.fromIterable(findAll()).flatMap(handler -> {
            Integer roles = handler.getRoles();
            String topology = handler.getAppMetadata().getTopology();
            Mono<Void> fireEvent;
            if ("internet".equals(topology)) {
                // add defaultUri for internet access for IoT devices
                // RSocketBroker defaultBroker = rsocketBrokerManager.findConsistentBroker(handler.getUuid());
                fireEvent = handler.fireCloudEventToPeer(brokerClusterAliasesEvent);
            } else {
                fireEvent = handler.fireCloudEventToPeer(brokerClustersEvent);
            }
            if (roles == 2) { // publish services only
                return fireEvent;
            } else if (roles == 3) { //consume and publish services
                return fireEvent.delayElement(Duration.ofSeconds(15));
            } else { //consume services
                return fireEvent.delayElement(Duration.ofSeconds(30));
            }
        });
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public JwtPrincipal appNameBasedPrincipal(String appName) {
        return new JwtPrincipal(UUID.randomUUID().toString(), appName,
                Arrays.asList("mock_owner"),
                new HashSet<>(Arrays.asList("admin")),
                Collections.emptySet(),
                new HashSet<>(Arrays.asList("default")),
                new HashSet<>(Arrays.asList("1"))
        );
    }

    /**
     * return rejected Rsocket with dispose logic
     *
     * @param errorMsg        error msg
     * @param requesterSocket requesterSocket
     * @return Mono with RejectedSetupException error
     */
    private Mono<RSocket> returnRejectedRSocket(@NotNull String errorMsg, @NotNull RSocket requesterSocket) {
        return Mono.<RSocket>error(new RejectedSetupException(errorMsg)).doFinally((signalType -> {
            if (requesterSocket.isDisposed()) {
                requesterSocket.dispose();
            }
        }));
    }

    @Nullable
    private RSocket getUpstreamRSocket() {
        if (applicationContext.containsBean("upstreamBrokerRSocket")) {
            try {
                return (RSocket) applicationContext.getBean("upstreamBrokerRSocket");
            } catch (Exception ignore) {
            }
        }
        return null;
    }
}
