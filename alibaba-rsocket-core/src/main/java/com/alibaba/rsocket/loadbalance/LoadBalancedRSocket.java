package com.alibaba.rsocket.loadbalance;

import com.alibaba.rsocket.AbstractRSocket;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.CloudEventRSocket;
import com.alibaba.rsocket.cloudevents.EventReply;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.exceptions.ConnectionErrorException;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.uri.UriTransportRegistry;
import io.rsocket.util.ByteBufPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.net.ConnectException;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;

/**
 * Load balanced RSocket
 *
 * @author leijuan
 */
public class LoadBalancedRSocket extends AbstractRSocket implements CloudEventRSocket {
    private static final Logger log = LoggerFactory.getLogger(LoadBalancedRSocket.class);
    private RandomSelector<RSocket> randomSelector;
    private final String serviceId;
    private final Flux<Collection<String>> urisFactory;
    private Collection<String> lastRSocketUris = new ArrayList<>();
    private Collection<String> firstBatchUris;
    private Map<String, RSocket> activeSockets;
    /**
     * unhealthy uris
     */
    private final Set<String> unHealthyUriSet = new HashSet<>();
    private long lastHealthCheckTimeStamp = System.currentTimeMillis();
    private long lastRefreshTimeStamp = System.currentTimeMillis();
    /**
     * health check interval seconds
     */
    private static final int HEALTH_CHECK_INTERVAL_SECONDS = 15;
    /**
     * retry count because of connection error and interval is 5 seconds
     */
    private final int retryCount = 12;
    private final RSocketRequesterSupport requesterSupport;
    private final ByteBuf healthCheckCompositeByteBuf;
    private boolean isServiceProvider = false;

    public Set<String> getUnHealthyUriSet() {
        return unHealthyUriSet;
    }

    public Collection<String> getLastRSocketUris() {
        return lastRSocketUris;
    }

    public long getLastHealthCheckTimeStamp() {
        return lastHealthCheckTimeStamp;
    }

    public long getLastRefreshTimeStamp() {
        return lastRefreshTimeStamp;
    }

    /**
     * connection error predicate
     */
    private static Predicate<? super Throwable> CONNECTION_ERROR_PREDICATE = e -> e instanceof ClosedChannelException || e instanceof ConnectionErrorException || e instanceof ConnectException;

    public LoadBalancedRSocket(String serviceId, Flux<Collection<String>> urisFactory,
                               RSocketRequesterSupport requesterSupport) {
        this.serviceId = serviceId;
        this.randomSelector = new RandomSelector<>(this.serviceId, new ArrayList<>());
        this.urisFactory = urisFactory;
        this.requesterSupport = requesterSupport;
        if (!requesterSupport.exposedServices().get().isEmpty()) {
            this.isServiceProvider = true;
        }
        this.activeSockets = new HashMap<>();
        this.urisFactory.subscribe(this::refreshRsockets);
        //composite metadata for health check
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(
                new GSVRoutingMetadata(null, RSocketServiceHealth.class.getCanonicalName(), "check", null),
                new MessageMimeTypeMetadata(RSocketMimeType.Hessian));
        ByteBuf compositeMetadataContent = compositeMetadata.getContent();
        this.healthCheckCompositeByteBuf = Unpooled.copiedBuffer(compositeMetadataContent);
        ReferenceCountUtil.safeRelease(compositeMetadataContent);
        //start health check timer
        startHealthCheckTimer();
        //reconnect unhealthy uris every 3 minutes
        checkUnhealthyUris();
    }

    private void refreshRsockets(Collection<String> rsocketUris) {
        //no changes
        if (isSameWithLastUris(rsocketUris)) {
            return;
        }
        //save first batch uris
        if (this.firstBatchUris == null) {
            firstBatchUris = rsocketUris;
        }
        log.info(RsocketErrorCode.message("RST-300207", serviceId, String.join(",", rsocketUris)));
        this.lastRefreshTimeStamp = System.currentTimeMillis();
        this.lastRSocketUris = rsocketUris;
        this.unHealthyUriSet.clear();
        Flux.fromIterable(rsocketUris)
                .flatMap(rsocketUri -> {
                    if (activeSockets.containsKey(rsocketUri)) {
                        return Mono.just(Tuples.of(rsocketUri, activeSockets.get(rsocketUri)));
                    } else {
                        return connect(rsocketUri)
                                //health check after connection
                                .flatMap(rsocket -> healthCheck(rsocket, rsocketUri).map(payload -> Tuples.of(rsocketUri, rsocket)))
                                .doOnError(error -> {
                                    log.error(RsocketErrorCode.message("RST-400500", rsocketUri), error);
                                    this.unHealthyUriSet.add(rsocketUri);
                                    tryToReconnect(rsocketUri, error);
                                });
                    }
                })
                .collectList()
                .subscribe(tupleRsockets -> {
                    if (tupleRsockets.isEmpty()) return;
                    Map<String, RSocket> newActiveRSockets = new HashMap<>();
                    for (Tuple2<String, RSocket> tuple : tupleRsockets) {
                        newActiveRSockets.put(tuple.getT1(), tuple.getT2());
                    }
                    @SuppressWarnings("DuplicatedCode")
                    Map<String, RSocket> staleRSockets = new HashMap<>();
                    //get all stale rsockets
                    for (Map.Entry<String, RSocket> entry : activeSockets.entrySet()) {
                        if (!newActiveRSockets.containsKey(entry.getKey())) {
                            staleRSockets.put(entry.getKey(), entry.getValue());
                        }
                    }
                    Map<String, RSocket> newAddedRSockets = new HashMap<>();
                    //get all new added rsockets
                    for (Map.Entry<String, RSocket> entry : newActiveRSockets.entrySet()) {
                        if (!activeSockets.containsKey(entry.getKey())) {
                            newAddedRSockets.put(entry.getKey(), entry.getValue());
                        }
                    }
                    this.activeSockets = newActiveRSockets;
                    this.randomSelector = new RandomSelector<>(this.serviceId, new ArrayList<>(activeSockets.values()));
                    //close all stale rsocket
                    if (!staleRSockets.isEmpty()) {
                        //Drain mode support, close consumer first, then provider
                        int delaySeconds = this.isServiceProvider ? 45 : 15;
                        Flux.fromIterable(staleRSockets.entrySet())
                                .delaySubscription(Duration.ofSeconds(delaySeconds))
                                .subscribe(entry -> {
                                    log.info(RsocketErrorCode.message("RST-200011", entry.getKey()));
                                    entry.getValue().dispose();
                                });
                    }
                    //subscribe rsocket close event
                    for (Map.Entry<String, RSocket> entry : newAddedRSockets.entrySet()) {
                        entry.getValue().onClose().subscribe(aVoid -> {
                            onRSocketClosed(entry.getKey(), entry.getValue(), null);
                        });
                    }
                });
    }

    @Override
    public @NotNull Mono<Payload> requestResponse(@NotNull Payload payload) {
        RSocket next = randomSelector.next();
        if (next == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new NoAvailableConnectionException(RsocketErrorCode.message("RST-200404", serviceId)));
        }
        return next.requestResponse(payload)
                .onErrorResume(CONNECTION_ERROR_PREDICATE, error -> {
                    onRSocketClosed(next, error);
                    return requestResponse(payload);
                });
    }


    @Override
    public @NotNull Mono<Void> fireAndForget(@NotNull Payload payload) {
        RSocket next = randomSelector.next();
        if (next == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new NoAvailableConnectionException(RsocketErrorCode.message("RST-200404", serviceId)));
        }
        return next.fireAndForget(payload)
                .onErrorResume(CONNECTION_ERROR_PREDICATE, error -> {
                    onRSocketClosed(next, error);
                    return fireAndForget(payload);
                });
    }

    @Override
    public Mono<Void> fireCloudEvent(CloudEventImpl<?> cloudEvent) {
        try {
            Payload payload = cloudEventToMetadataPushPayload(cloudEvent);
            return metadataPush(payload);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Void> fireEventReply(URI replayTo, EventReply eventReply) {
        return fireAndForget(constructEventReplyPayload(replayTo, eventReply));
    }

    /**
     * fire cloud event to upstream active rsockets
     *
     * @param cloudEvent cloud event
     * @return void
     */
    public Mono<Void> fireCloudEventToUpstreamAll(CloudEventImpl<?> cloudEvent) {
        try {
            return Flux.fromIterable(this.getActiveSockets().values())
                    .flatMap(rsocket -> rsocket.metadataPush(cloudEventToMetadataPushPayload(cloudEvent)))
                    .doOnError(throwable -> log.error(RsocketErrorCode.message("RST-610502"), throwable))
                    .then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @Override
    public @NotNull Flux<Payload> requestStream(@NotNull Payload payload) {
        RSocket next = randomSelector.next();
        if (next == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Flux.error(new NoAvailableConnectionException(RsocketErrorCode.message("RST-200404", serviceId)));
        }
        return next.requestStream(payload)
                .onErrorResume(CONNECTION_ERROR_PREDICATE, error -> {
                    onRSocketClosed(next, error);
                    return requestStream(payload);
                });
    }

    @Override
    public @NotNull Flux<Payload> requestChannel(@NotNull Publisher<Payload> payloads) {
        RSocket next = randomSelector.next();
        if (next == null) {
            return Flux.error(new NoAvailableConnectionException(RsocketErrorCode.message("RST-200404", serviceId)));
        }
        return next.requestChannel(payloads)
                .onErrorResume(CONNECTION_ERROR_PREDICATE, error -> {
                    onRSocketClosed(next, error);
                    return requestChannel(payloads);
                });
    }

    @Override
    public @NotNull Mono<Void> metadataPush(final @NotNull Payload payload) {
        return Flux.fromIterable(activeSockets.values()).flatMap(rSocket -> rSocket.metadataPush(payload)).then();
    }

    public void dispose() {
        super.dispose();
        for (RSocket rsocket : activeSockets.values()) {
            try {
                rsocket.dispose();
            } catch (Exception ignore) {

            }
        }
        activeSockets.clear();
    }

    public Map<String, RSocket> getActiveSockets() {
        return activeSockets;
    }

    public void refreshUnHealthyUris() {
        for (String unHealthyUri : unHealthyUriSet) {
            tryToReconnect(unHealthyUri, null);
        }
    }

    public void onRSocketClosed(RSocket rsocket, @Nullable Throwable cause) {
        for (Map.Entry<String, RSocket> entry : activeSockets.entrySet()) {
            if (entry.getValue() == rsocket) {
                onRSocketClosed(entry.getKey(), entry.getValue(), null);
            }
        }
        if (!rsocket.isDisposed()) {
            try {
                rsocket.dispose();
            } catch (Exception ignore) {

            }
        }
    }

    public void onRSocketClosed(String uri, @Nullable Throwable cause) {
        if (activeSockets.containsKey(uri)) {
            onRSocketClosed(uri, activeSockets.get(uri), cause);
        }
    }

    public void onRSocketClosed(String rsocketUri, RSocket rsocket, @Nullable Throwable cause) {
        //in last rsocket uris or not
        if (this.lastRSocketUris.contains(rsocketUri)) {
            this.unHealthyUriSet.add(rsocketUri);
            if (activeSockets.containsKey(rsocketUri)) {
                activeSockets.remove(rsocketUri);
                this.randomSelector = new RandomSelector<>(this.serviceId, new ArrayList<>(activeSockets.values()));
                log.error(RsocketErrorCode.message("RST-500407", rsocketUri));
                tryToReconnect(rsocketUri, cause);
            }
            if (!rsocket.isDisposed()) {
                try {
                    rsocket.dispose();
                } catch (Exception ignore) {

                }
            }
        }
        // use first batch uris to refresh connections
        if (activeSockets.isEmpty() && !lastRSocketUris.containsAll(firstBatchUris)) {
            refreshRsockets(firstBatchUris);
        }
    }

    public void onRSocketReconnected(String rsocketUri, RSocket rsocket) {
        this.activeSockets.put(rsocketUri, rsocket);
        this.unHealthyUriSet.remove(rsocketUri);
        this.randomSelector = new RandomSelector<>(this.serviceId, new ArrayList<>(activeSockets.values()));
        rsocket.onClose().subscribe(aVoid -> onRSocketClosed(rsocketUri, rsocket, null));
        CloudEventImpl<ServicesExposedEvent> cloudEvent = requesterSupport.servicesExposedEvent().get();
        if (cloudEvent != null) {
            try {
                Payload payload = cloudEventToMetadataPushPayload(cloudEvent);
                rsocket.metadataPush(payload).subscribe();
            } catch (Exception ignore) {

            }
        }
    }

    public void tryToReconnect(String rsocketUri, @Nullable Throwable error) {
        //try to reconnect every 5 seconds in 1 minute if connection error
        if (CONNECTION_ERROR_PREDICATE.test(error)) {
            Flux.range(1, retryCount)
                    .delayElements(Duration.ofSeconds(5))
                    .filter(id -> activeSockets.isEmpty() || !activeSockets.containsKey(rsocketUri))
                    .subscribe(number -> {
                        connect(rsocketUri)
                                .flatMap(rsocket -> healthCheck(rsocket, rsocketUri).map(payload -> rsocket))
                                .doOnError(e -> {
                                    this.getUnHealthyUriSet().add(rsocketUri);
                                    log.error(RsocketErrorCode.message("RST-500408", number, rsocketUri), e);
                                })
                                .subscribe(rsocket -> {
                                    onRSocketReconnected(rsocketUri, rsocket);
                                    log.info(RsocketErrorCode.message("RST-500203", rsocketUri));
                                });
                    });
        }
    }

    Mono<RSocket> connect(String uri) {
        try {
            RSocketConnector rsocketConnector = RSocketConnector.create();
            for (RSocketInterceptor requestInterceptor : requesterSupport.requestInterceptors()) {
                rsocketConnector.interceptors(interceptorRegistry -> {
                    interceptorRegistry.forRequester(requestInterceptor);
                });
            }
            for (RSocketInterceptor responderInterceptor : requesterSupport.responderInterceptors()) {
                rsocketConnector.interceptors(interceptorRegistry -> {
                    interceptorRegistry.forResponder(responderInterceptor);
                });
            }
            Payload payload = requesterSupport.setupPayload().get();
            return rsocketConnector
                    .setupPayload(payload)
                    .metadataMimeType(RSocketMimeType.CompositeMetadata.getType())
                    .dataMimeType(RSocketMimeType.Hessian.getType())
                    .acceptor(requesterSupport.socketAcceptor())
                    .connect(UriTransportRegistry.clientForUri(uri));
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-400500", uri), e);
            return Mono.error(new ConnectionErrorException(uri));
        }
    }

    /**
     * start health check timer: check the connection every 15 seconds
     * please check https://github.com/alibaba/alibaba-rsocket-broker/issues/10
     */
    public void startHealthCheckTimer() {
        this.lastHealthCheckTimeStamp = System.currentTimeMillis();
        Flux.interval(Duration.ofSeconds(HEALTH_CHECK_INTERVAL_SECONDS))
                .flatMap(timestamp -> Flux.fromIterable(activeSockets.entrySet()))
                .subscribe(entry -> {
                    healthCheck(entry.getValue(), entry.getKey()).doOnError(error -> {
                        if (CONNECTION_ERROR_PREDICATE.test(error)) { //connection closed
                            onRSocketClosed(entry.getKey(), entry.getValue(), error);
                        }
                    }).subscribe();
                });
    }

    public void checkUnhealthyUris() {
        Flux.interval(Duration.ofMinutes(5))
                .filter(sequence -> !unHealthyUriSet.isEmpty())
                .subscribe(entry -> {
                    for (String unhealthyUri : unHealthyUriSet) {
                        if (!activeSockets.containsKey(unhealthyUri)) {
                            connect(unhealthyUri)
                                    .flatMap(rsocket -> healthCheck(rsocket, unhealthyUri).map(payload -> rsocket))
                                    .subscribe(rsocket -> {
                                        onRSocketReconnected(unhealthyUri, rsocket);
                                        log.info(RsocketErrorCode.message("RST-500203", unhealthyUri));
                                    });
                        }
                    }
                });
    }

    private Mono<Boolean> healthCheck(RSocket rsocket, String url) {
        return rsocket.requestResponse(ByteBufPayload.create(Unpooled.EMPTY_BUFFER, this.healthCheckCompositeByteBuf.retainedDuplicate()))
                .timeout(Duration.ofSeconds(15)).handle((payload, sink) -> {
                    byte indicator = payload.data().readByte();
                    // check error code: hessian decode: 1: -111,  0-> -112, -1 -> -113
                    if (indicator == -111) {
                        sink.next(true);
                    } else {
                        sink.error(new Exception("Health check failed :" + url));
                    }
                });
    }

    public boolean isSameWithLastUris(Collection<String> newRSocketUris) {
        return this.lastRSocketUris.size() == newRSocketUris.size() && this.lastRSocketUris.containsAll(newRSocketUris) && newRSocketUris.containsAll(this.lastRSocketUris);
    }

}
