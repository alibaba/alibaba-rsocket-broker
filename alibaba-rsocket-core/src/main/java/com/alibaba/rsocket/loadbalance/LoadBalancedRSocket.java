package com.alibaba.rsocket.loadbalance;

import com.alibaba.rsocket.PayloadUtils;
import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.cloudevents.CloudEventRSocket;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.health.RSocketServiceHealth;
import com.alibaba.rsocket.listen.CompositeMetadataInterceptor;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.cloudevents.v1.CloudEventImpl;
import io.netty.buffer.Unpooled;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.exceptions.ConnectionErrorException;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.uri.UriTransportRegistry;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;

/**
 * Load balanced RSocket:  how to remove failure nodes?
 *
 * @author leijuan
 */
public class LoadBalancedRSocket extends AbstractRSocket implements CloudEventRSocket {
    private RandomSelector<RSocket> randomSelector;
    private Logger log = LoggerFactory.getLogger(LoadBalancedRSocket.class);
    private String serviceId;
    private Flux<Collection<String>> urisFactory;
    private Map<String, RSocket> activeSockets;
    private int retryCount = 3;
    /**
     * un health uri set
     */
    private Set<String> unHealthUriSet = new HashSet<>();
    private long lastHealthCheckTimeStamp = System.currentTimeMillis();
    private long lastRefreshTimeStamp = System.currentTimeMillis();
    /**
     * health check interval seconds
     */
    private static int HEALTH_CHECK_INTERVAL_SECONDS = 15;
    private RSocketRequesterSupport requesterSupport;

    public Set<String> getUnHealthUriSet() {
        return unHealthUriSet;
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
    private static Predicate<? super Throwable> CONNECTION_ERROR_PREDICATE = e -> e instanceof ClosedChannelException || e instanceof ConnectionErrorException;

    public LoadBalancedRSocket(String serviceId, Flux<Collection<String>> urisFactory,
                               RSocketRequesterSupport requesterSupport) {
        this.serviceId = serviceId;
        this.randomSelector = new RandomSelector<>(this.serviceId, new ArrayList<>());
        this.urisFactory = urisFactory;
        this.requesterSupport = requesterSupport;
        this.activeSockets = new HashMap<>();
        this.urisFactory.subscribe(this::refreshRsockets);
        startHealthCheckTimer();
    }

    private void refreshRsockets(Collection<String> rsocketUris) {
        this.lastRefreshTimeStamp = System.currentTimeMillis();
        this.unHealthUriSet.clear();
        Flux.fromIterable(rsocketUris)
                .flatMap(rsocketUri -> {
                    if (activeSockets.containsKey(rsocketUri)) {
                        return Mono.just(Tuples.of(rsocketUri, activeSockets.get(rsocketUri)));
                    } else {
                        return connect(rsocketUri)
                                .doOnError(e -> {
                                    log.error(RsocketErrorCode.message("RST-400500", rsocketUri), e);
                                    tryToReconnect(rsocketUri);
                                }).map(rSocket -> Tuples.of(rsocketUri, rSocket));
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
                    //close all stale rsocket after 15 for drain mode
                    if (!staleRSockets.isEmpty()) {
                        Flux.fromIterable(staleRSockets.entrySet())
                                .delaySubscription(Duration.ofSeconds(15))
                                .subscribe(entry -> {
                                    try {
                                        entry.getValue().dispose();
                                        log.info(RsocketErrorCode.message("RST-200011", entry.getKey()));
                                    } catch (Exception ignore) {

                                    }
                                });
                    }
                    //subscribe rsocket close event
                    for (Map.Entry<String, RSocket> entry : newAddedRSockets.entrySet()) {
                        entry.getValue().onClose().subscribe(aVoid -> {
                            onRSocketClosed(entry.getKey(), entry.getValue());
                        });
                    }
                });
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        return Mono.defer(randomSelector)
                .flatMap(rsocket -> rsocket.requestResponse(payload)
                        .doOnError(CONNECTION_ERROR_PREDICATE, error -> onRSocketClosed(rsocket)))
                .retry(retryCount, CONNECTION_ERROR_PREDICATE);
    }


    @Override
    public Mono<Void> fireAndForget(Payload payload) {
        RSocket next = randomSelector.next();
        if (next == null) {
            return Mono.error(new NoAvailableConnectionException(RsocketErrorCode.message("RST-200404", serviceId)));
        }
        return Mono.defer(randomSelector)
                .flatMap(rsocket -> rsocket.fireAndForget(payload)
                        .doOnError(CONNECTION_ERROR_PREDICATE, error -> onRSocketClosed(next)))
                .retry(retryCount, CONNECTION_ERROR_PREDICATE);
    }

    @Override
    public Mono<Void> fireCloudEvent(CloudEventImpl<?> cloudEvent) {
        try {
            Payload payload = PayloadUtils.cloudEventToMetadataPushPayload(cloudEvent);
            return metadataPush(payload);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
        return Mono.defer(randomSelector)
                .flatMapMany(rsocket -> rsocket.requestStream(payload)
                        .doOnError(CONNECTION_ERROR_PREDICATE, error -> onRSocketClosed(rsocket)))
                .retry(retryCount, CONNECTION_ERROR_PREDICATE);
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return Mono.defer(randomSelector)
                .flatMapMany(rsocket -> rsocket.requestChannel(payloads)
                        .doOnError(CONNECTION_ERROR_PREDICATE, error -> onRSocketClosed(rsocket)))
                .retry(retryCount, CONNECTION_ERROR_PREDICATE);
    }

    @Override
    public Mono<Void> metadataPush(final Payload payload) {
        return Flux.fromIterable(activeSockets.values()).flatMap(rSocket -> rSocket.metadataPush(payload)).then();
    }

    public void dispose() {
        synchronized (this) {
            super.dispose();
            Flux.fromIterable(activeSockets.values())
                    .doOnTerminate(() -> activeSockets.clear())
                    .subscribe(Disposable::dispose);
        }
    }

    public Map<String, RSocket> getActiveSockets() {
        return activeSockets;
    }


    public void onRSocketClosed(RSocket rsocket) {
        for (Map.Entry<String, RSocket> entry : activeSockets.entrySet()) {
            if (entry.getValue() == rsocket) {
                onRSocketClosed(entry.getKey(), entry.getValue());
            }
        }
        if (!rsocket.isDisposed()) {
            try {
                rsocket.dispose();
            } catch (Exception ignore) {

            }
        }
    }

    public void onRSocketClosed(String rsocketUri, RSocket rsocket) {
        this.unHealthUriSet.add(rsocketUri);
        if (activeSockets.containsKey(rsocketUri)) {
            activeSockets.remove(rsocketUri);
            this.randomSelector = new RandomSelector<>(this.serviceId, new ArrayList<>(activeSockets.values()));
            log.error(RsocketErrorCode.message("RST-500407", rsocketUri));
            tryToReconnect(rsocketUri);
        }
        if (!rsocket.isDisposed()) {
            try {
                rsocket.dispose();
            } catch (Exception ignore) {

            }
        }
    }

    public void onRSocketReconnected(String rsocketUri, RSocket rsocket) {
        this.activeSockets.put(rsocketUri, rsocket);
        this.unHealthUriSet.remove(rsocketUri);
        this.randomSelector = new RandomSelector<>(this.serviceId, new ArrayList<>(activeSockets.values()));
        rsocket.onClose().subscribe(aVoid -> {
            onRSocketClosed(rsocketUri, rsocket);
        });
        CloudEventImpl<ServicesExposedEvent> cloudEvent = requesterSupport.servicesExposedEvent().get();
        if (cloudEvent != null) {
            try {
                Payload payload = PayloadUtils.cloudEventToMetadataPushPayload(cloudEvent);
                rsocket.metadataPush(payload).subscribe();
            } catch (Exception ignore) {

            }
        }
    }

    public void tryToReconnect(String rsocketUri) {
        //try to reconnect every 5 seconds in 1 minute
        Flux.range(1, 12)
                .delayElements(Duration.ofSeconds(5))
                .subscribe(number -> {
                    if (!activeSockets.containsKey(rsocketUri)) {
                        connect(rsocketUri)
                                .doOnError(e -> {
                                    this.getUnHealthUriSet().add(rsocketUri);
                                    log.error(RsocketErrorCode.message("RST-500408", number, rsocketUri), e);
                                })
                                .subscribe(rsocket -> {
                                    onRSocketReconnected(rsocketUri, rsocket);
                                    log.info(RsocketErrorCode.message("RST-500203", rsocketUri));
                                });
                    }
                });
    }

    Mono<RSocket> connect(String uri) {
        try {
            RSocketFactory.ClientRSocketFactory clientRSocketFactory = RSocketFactory.connect();
            for (RSocketInterceptor requestInterceptor : requesterSupport.requestInterceptors()) {
                clientRSocketFactory = clientRSocketFactory.addRequesterPlugin(requestInterceptor);
            }
            clientRSocketFactory = clientRSocketFactory.addResponderPlugin(CompositeMetadataInterceptor.getInstance());
            for (RSocketInterceptor responderInterceptor : requesterSupport.responderInterceptors()) {
                clientRSocketFactory = clientRSocketFactory.addResponderPlugin(responderInterceptor);
            }
            return clientRSocketFactory
                    .keepAliveMissedAcks(12)
                    .setupPayload(requesterSupport.setupPayload().get())
                    .metadataMimeType(RSocketAppContext.DEFAULT_METADATA_TYPE)
                    .dataMimeType(RSocketAppContext.DEFAULT_DATA_TYPE)
                    .errorConsumer(error -> {
                        log.error(RsocketErrorCode.message("RST-200501"), error);
                    })
                    .acceptor(requesterSupport.socketAcceptor())
                    .transport(UriTransportRegistry.clientForUri(uri))
                    .start();
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
                    GSVRoutingMetadata routing = new GSVRoutingMetadata("", RSocketServiceHealth.class.getCanonicalName(), "check", "");
                    MessageMimeTypeMetadata encoding = new MessageMimeTypeMetadata(RSocketMimeType.Hessian);
                    RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(routing, encoding);
                    Mono<Payload> mono = entry.getValue().requestResponse(DefaultPayload.create(Unpooled.EMPTY_BUFFER, compositeMetadata.getContent()));
                    mono.doOnError(error -> {
                        if (error instanceof ClosedChannelException) { //connection closed
                            onRSocketClosed(entry.getKey(), entry.getValue());
                        }
                    }).subscribe();
                });
    }

}
