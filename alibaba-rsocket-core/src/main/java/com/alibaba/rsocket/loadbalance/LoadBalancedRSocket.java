package com.alibaba.rsocket.loadbalance;

import com.alibaba.rsocket.PayloadUtils;
import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.cloudevents.CloudEventRSocket;
import com.alibaba.rsocket.events.ServicesExposedEvent;
import com.alibaba.rsocket.listen.CompositeMetadataInterceptor;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.cloudevents.v1.CloudEventImpl;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.exceptions.ConnectionErrorException;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.uri.UriTransportRegistry;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Load balanced RSocket:  how to remove failure nodes?
 *
 * @author leijuan
 */
public class LoadBalancedRSocket extends AbstractRSocket implements CloudEventRSocket {
    private RandomSelector<RSocket> randomSelector = new RandomSelector<>(new ArrayList<>());
    private Logger log = LoggerFactory.getLogger(LoadBalancedRSocket.class);
    private String serviceId;
    private Flux<Collection<String>> urisFactory;
    private Map<String, RSocket> activeSockets;
    private long lastRefresh;
    private RSocketRequesterSupport requesterSupport;

    public LoadBalancedRSocket(String serviceId, Flux<Collection<String>> urisFactory,
                               RSocketRequesterSupport requesterSupport) {
        this.serviceId = serviceId;
        this.urisFactory = urisFactory;
        this.requesterSupport = requesterSupport;
        this.activeSockets = new HashMap<>();
        this.urisFactory.subscribe(this::refreshRsockets);
    }

    private void refreshRsockets(Collection<String> rsocketUris) {
        this.lastRefresh = System.currentTimeMillis();
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
                    Map<String, RSocket> staleRSockets = new HashMap<>();
                    //get all stale rsockets
                    for (Map.Entry<String, RSocket> entry : activeSockets.entrySet()) {
                        if (!newActiveRSockets.containsKey(entry.getKey())) {
                            staleRSockets.put(entry.getKey(), entry.getValue());
                        }
                    }
                    this.activeSockets = newActiveRSockets;
                    this.randomSelector = new RandomSelector<>(new ArrayList<>(activeSockets.values()));
                    //close all stale rsocket after 15 for drain mode
                    Flux.fromIterable(staleRSockets.entrySet())
                            .delaySubscription(Duration.ofSeconds(15))
                            .subscribe(entry -> {
                                try {
                                    entry.getValue().dispose();
                                    log.info(RsocketErrorCode.message("RST-200011", entry.getKey()));
                                } catch (Exception ignore) {

                                }
                            });
                    //subscribe rsocket close event
                    for (Map.Entry<String, RSocket> entry : activeSockets.entrySet()) {
                        entry.getValue().onClose().subscribe(aVoid -> {
                            onRSocketClosed(entry.getKey(), entry.getValue());
                        });
                    }
                });
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        RSocket next = randomSelector.next();
        if (next == null) {
            return Mono.error(new ConnectionErrorException(RsocketErrorCode.message("RST-200404", serviceId)));
        }
        return next.requestResponse(payload);
    }


    @Override
    public Mono<Void> fireAndForget(Payload payload) {
        RSocket next = randomSelector.next();
        if (next == null) {
            return Mono.error(new ConnectionErrorException(RsocketErrorCode.message("RST-200404", serviceId)));
        }
        return next.fireAndForget(payload);
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
        RSocket next = randomSelector.next();
        if (next == null) {
            return Flux.error(new ConnectionErrorException(RsocketErrorCode.message("RST-200404", serviceId)));
        }
        return next.requestStream(payload);
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        RSocket next = randomSelector.next();
        if (next == null) {
            return Flux.error(new ConnectionErrorException(RsocketErrorCode.message("RST-200404", serviceId)));
        }
        return next.requestChannel(payloads);
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

    public void onRSocketClosed(String rsocketUri, RSocket rsocket) {
        activeSockets.remove(rsocketUri);
        this.randomSelector = new RandomSelector<>(new ArrayList<>(activeSockets.values()));
        log.error(RsocketErrorCode.message("RST-500407", rsocketUri));
        tryToReconnect(rsocketUri);
    }

    public void onRSocketReconnected(String rsocketUri, RSocket rsocket) {
        activeSockets.put(rsocketUri, rsocket);
        this.randomSelector = new RandomSelector<>(new ArrayList<>(activeSockets.values()));
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
        Flux.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
                .delayElements(Duration.ofSeconds(5))
                .subscribe(number -> {
                    if (!activeSockets.containsKey(rsocketUri)) {
                        connect(rsocketUri)
                                .doOnError(e -> {
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
                    .acceptor(requesterSupport.socketAcceptor())
                    .transport(UriTransportRegistry.clientForUri(uri))
                    .start();
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-400500", uri), e);
            return Mono.error(new ConnectionErrorException(uri));
        }
    }

}
