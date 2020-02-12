package com.alibaba.spring.boot.rsocket.broker.responder;

import com.alibaba.rsocket.RSocketAppContext;
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
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBroker;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceMeshInspector;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.alibaba.spring.boot.rsocket.broker.security.AuthenticationService;
import com.alibaba.spring.boot.rsocket.broker.security.JwtPrincipal;
import com.alibaba.spring.boot.rsocket.broker.security.RSocketAppPrincipal;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.exceptions.InvalidSetupException;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.extra.processor.TopicProcessor;

import java.net.URI;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private TopicProcessor<CloudEventImpl> eventProcessor;
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
    private RSocketBrokerManager rSocketBrokerManager;
    private ServiceMeshInspector serviceMeshInspector;
    private boolean authRequired;

    public RSocketBrokerHandlerRegistryImpl(LocalReactiveServiceCaller localReactiveServiceCaller, RSocketFilterChain rsocketFilterChain,
                                            ServiceRoutingSelector routingSelector,
                                            TopicProcessor<CloudEventImpl> eventProcessor,
                                            AuthenticationService authenticationService,
                                            RSocketBrokerManager rSocketBrokerManager,
                                            ServiceMeshInspector serviceMeshInspector,
                                            boolean authRequired) {
        this.localReactiveServiceCaller = localReactiveServiceCaller;
        this.rsocketFilterChain = rsocketFilterChain;
        this.routingSelector = routingSelector;
        this.eventProcessor = eventProcessor;
        this.authenticationService = authenticationService;
        this.rSocketBrokerManager = rSocketBrokerManager;
        this.serviceMeshInspector = serviceMeshInspector;
        this.authRequired = authRequired;
        if (!rSocketBrokerManager.isStandAlone()) {
            this.rSocketBrokerManager.requestAll().flatMap(this::broadcastClusterTopology).subscribe();
        }
    }

    @Override
    @Nullable
    public Mono<RSocket> accept(ConnectionSetupPayload setupPayload, RSocket sendingSocket) {
        //parse setup payload
        RSocketCompositeMetadata compositeMetadata;
        AppMetadata appMetadata = null;
        String credentials = "";
        RSocketAppPrincipal principal = null;
        try {
            compositeMetadata = RSocketCompositeMetadata.from(setupPayload.metadata());
            if (!authRequired) {  //authentication not required
                principal = appNameBasedPrincipal("MockApp");
                credentials = UUID.randomUUID().toString();
            } else if (compositeMetadata.contains(RSocketMimeType.BearerToken)) {
                BearerTokenMetadata bearerTokenMetadata = BearerTokenMetadata.from(compositeMetadata.getMetadata(RSocketMimeType.BearerToken));
                credentials = new String(bearerTokenMetadata.getBearerToken());
                principal = authenticationService.auth("JWT", credentials);
            }
            //validate application information
            if (principal != null && compositeMetadata.contains(RSocketMimeType.Application)) {
                AppMetadata temp = AppMetadata.from(compositeMetadata.getMetadata(RSocketMimeType.Application));
                //App registration validation: app id: UUID and unique in server
                String appId = temp.getUuid();
                Integer instanceId = MurmurHash3.hash32(credentials + ":" + temp.getUuid());
                temp.setId(instanceId);
                if (appId != null && appId.length() >= 32 && !routingSelector.containInstance(instanceId)) {
                    appMetadata = temp;
                    appMetadata.setConnectedAt(new Date());
                }
            }
            //Security authentication
            if (appMetadata != null) {
                appMetadata.addMetadata("_orgs", String.join(",", principal.getOrganizations()));
                appMetadata.addMetadata("_roles", String.join(",", principal.getRoles()));
                appMetadata.addMetadata("_serviceAccounts", String.join(",", principal.getServiceAccounts()));
            }
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-500402"), e);
            ReferenceCountUtil.safeRelease(setupPayload);
            return Mono.error(new InvalidSetupException("RST-500405"));
        }
        //validate connection legal or not
        if (principal == null) {
            final String message = RsocketErrorCode.message("RST-500405");
            log.error(message);
            ReferenceCountUtil.safeRelease(setupPayload);
            return Mono.error(new InvalidSetupException(message));
        }
        //create handler
        try {
            RSocketBrokerResponderHandler brokerResponderHandler = new RSocketBrokerResponderHandler(setupPayload, compositeMetadata, appMetadata, principal,
                    sendingSocket, routingSelector, eventProcessor, this, serviceMeshInspector);
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
            log.error(RsocketErrorCode.message("RST-500406"), e);
            ReferenceCountUtil.safeRelease(setupPayload);
            return Mono.error(new InvalidSetupException("RST-500406: " + e.getMessage()));
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
        responderHandlers.put(responderHandler.getAppMetadata().getUuid(), responderHandler);
        connectionHandlers.put(responderHandler.getId(), responderHandler);
        appHandlers.put(responderHandler.getAppMetadata().getName(), responderHandler);
        eventProcessor.onNext(appStatusEventCloudEvent(responderHandler.getAppMetadata(), AppStatusEvent.STATUS_CONNECTED));
        if (!rSocketBrokerManager.isStandAlone()) {
            responderHandler.fireCloudEventToPeer(getBrokerClustersEvent(rSocketBrokerManager.currentBrokers())).subscribe();
        }
    }

    @Override
    public void onHandlerDisposed(RSocketBrokerResponderHandler responderHandler) {
        responderHandlers.remove(responderHandler.getUuid());
        connectionHandlers.remove(responderHandler.getId());
        appHandlers.remove(responderHandler.getAppMetadata().getName(), responderHandler);
        log.info(RsocketErrorCode.message("RST-500202"));
        eventProcessor.onNext(appStatusEventCloudEvent(responderHandler.getAppMetadata(), AppStatusEvent.STATUS_STOPPED));
    }

    @Override
    public Multimap<String, RSocketBrokerResponderHandler> appHandlers() {
        return appHandlers;
    }

    @Override
    public Mono<Void> broadcast(String appName, final CloudEventImpl cloudEvent) {
        if (appHandlers.containsKey(appName)) {
            return Flux.fromIterable(appHandlers.get(appName))
                    .flatMap(handler -> handler.fireCloudEventToPeer(cloudEvent))
                    .then();
        } else if (appName.equals("*")) {
            return Flux.fromIterable(appHandlers.keySet())
                    .flatMap(name -> Flux.fromIterable(appHandlers.get(name)))
                    .flatMap(handler -> handler.fireCloudEventToPeer(cloudEvent))
                    .then();
        } else {
            return Mono.empty();
        }
    }

    public void cleanStaleHandlers() {
        //todo clean stale handlers
        /*Flux.interval(Duration.ofSeconds(10)).subscribe(t -> {

        });*/
    }

    private CloudEventImpl<AppStatusEvent> appStatusEventCloudEvent(AppMetadata appMetadata, Integer status) {
        return CloudEventBuilder.<AppStatusEvent>builder()
                .withId(UUID.randomUUID().toString())
                .withTime(ZonedDateTime.now())
                .withSource(URI.create("app://" + appMetadata.getUuid()))
                .withType(AppStatusEvent.class.getCanonicalName())
                .withDataContentType("application/json")
                .withData(new AppStatusEvent(appMetadata.getUuid(), status))
                .build();
    }

    private CloudEventImpl<UpstreamClusterChangedEvent> getBrokerClustersEvent(Collection<RSocketBroker> rSocketBrokers) {
        List<String> uris = rSocketBrokers.stream()
                .filter(RSocketBroker::isActive)
                .map(RSocketBroker::getUrl)
                .collect(Collectors.toList());
        UpstreamClusterChangedEvent upstreamClusterChangedEvent = new UpstreamClusterChangedEvent();
        upstreamClusterChangedEvent.setGroup("");
        upstreamClusterChangedEvent.setInterfaceName("*");
        upstreamClusterChangedEvent.setVersion("");
        upstreamClusterChangedEvent.setUris(uris);

        // passing in the given attributes
        return CloudEventBuilder.<UpstreamClusterChangedEvent>builder()
                .withType("com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent")
                .withId(UUID.randomUUID().toString())
                .withTime(ZonedDateTime.now())
                .withDataschema(URI.create("rsocket:event:com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent"))
                .withDataContentType("application/json")
                .withSource(URI.create("broker://" + RSocketAppContext.ID))
                .withData(upstreamClusterChangedEvent)
                .build();
    }

    private Flux<Void> broadcastClusterTopology(Collection<RSocketBroker> rSocketBrokers) {
        final CloudEventImpl<UpstreamClusterChangedEvent> brokerClustersEvent = getBrokerClustersEvent(rSocketBrokers);
        return Flux.fromIterable(findAll()).flatMap(handler -> {
            Integer roles = handler.getRoles();
            Mono<Void> fireEvent = handler.fireCloudEventToPeer(brokerClustersEvent);
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
        return new JwtPrincipal(appName,
                Arrays.asList("mock_owner"),
                new HashSet<>(Arrays.asList("admin")),
                Collections.emptySet(),
                new HashSet<>(Arrays.asList("default")),
                new HashSet<>(Arrays.asList("1"))
        );
    }
}
