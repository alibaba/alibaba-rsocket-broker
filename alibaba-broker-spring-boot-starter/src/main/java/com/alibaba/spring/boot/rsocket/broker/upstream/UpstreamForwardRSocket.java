package com.alibaba.spring.boot.rsocket.broker.upstream;

import com.alibaba.rsocket.AbstractRSocket;
import com.alibaba.rsocket.RSocketExchange;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.Json;
import com.alibaba.rsocket.events.CloudEventSupport;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.rsocket.metadata.BinaryRoutingMetadata;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.route.RSocketFilterChain;
import com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.exceptions.ApplicationErrorException;
import io.rsocket.exceptions.InvalidException;
import io.rsocket.frame.FrameType;
import io.rsocket.metadata.WellKnownMimeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler.metrics;

/**
 * upstream request forward RSocket
 *
 * @author leijuan
 */
public class UpstreamForwardRSocket extends AbstractRSocket {
    private static Logger log = LoggerFactory.getLogger(UpstreamForwardRSocket.class);
    TypeReference<CloudEventImpl<ObjectNode>> CLOUD_EVENT_TYPE_REFERENCE = new TypeReference<CloudEventImpl<ObjectNode>>() {
    };
    private static final byte BINARY_ROUTING_MARK = (byte) (WellKnownMimeType.MESSAGE_RSOCKET_BINARY_ROUTING.getIdentifier() | 0x80);
    private ServiceRoutingSelector routingSelector;
    private RSocketBrokerHandlerRegistry handlerRegistry;
    private RSocketFilterChain filterChain;
    private AppMetadata upstreamBrokerMetadata;
    private RSocket peerRSocket;
    private UpstreamBrokerCluster upstreamBrokerCluster;

    public UpstreamForwardRSocket(ServiceRoutingSelector serviceRoutingSelector,
                                  RSocket peerRSocket,
                                  RSocketFilterChain filterChain,
                                  RSocketBrokerHandlerRegistry handlerRegistry,
                                  UpstreamBrokerCluster upstreamBrokerCluster) {
        this.routingSelector = serviceRoutingSelector;
        this.filterChain = filterChain;
        this.handlerRegistry = handlerRegistry;
        this.peerRSocket = peerRSocket;
        this.upstreamBrokerCluster = upstreamBrokerCluster;
        this.upstreamBrokerMetadata = new AppMetadata();
        this.upstreamBrokerMetadata.setName("CentralBroker");
    }

    @Override
    public @NotNull Mono<Void> fireAndForget(@NotNull Payload payload) {
        BinaryRoutingMetadata binaryRoutingMetadata = binaryRoutingMetadata(payload.metadata());
        GSVRoutingMetadata gsvRoutingMetadata;
        if (binaryRoutingMetadata != null) {
            gsvRoutingMetadata = GSVRoutingMetadata.from(new String(binaryRoutingMetadata.getRoutingText(), StandardCharsets.UTF_8));
        } else {
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
            gsvRoutingMetadata = compositeMetadata.getRoutingMetaData();
            if (gsvRoutingMetadata == null) {
                return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
            }
        }
        //request filters
        Mono<RSocket> destination = findDestination(gsvRoutingMetadata);
        if (this.filterChain.isFiltersPresent()) {
            RSocketExchange exchange = new RSocketExchange(FrameType.REQUEST_FNF, gsvRoutingMetadata, payload, this.upstreamBrokerMetadata);
            destination = filterChain.filter(exchange).then(destination);
        }
        //call destination
        return destination.flatMap(rsocket -> {
            metrics(gsvRoutingMetadata, "0x05");
            return rsocket.fireAndForget(payload);
        });
    }

    @Override
    public @NotNull Mono<Payload> requestResponse(@NotNull Payload payload) {
        BinaryRoutingMetadata binaryRoutingMetadata = binaryRoutingMetadata(payload.metadata());
        GSVRoutingMetadata gsvRoutingMetadata;
        if (binaryRoutingMetadata != null) {
            gsvRoutingMetadata = GSVRoutingMetadata.from(new String(binaryRoutingMetadata.getRoutingText(), StandardCharsets.UTF_8));
        } else {
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
            gsvRoutingMetadata = compositeMetadata.getRoutingMetaData();
            if (gsvRoutingMetadata == null) {
                return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
            }
        }
        //request filters
        Mono<RSocket> destination = findDestination(gsvRoutingMetadata);
        if (this.filterChain.isFiltersPresent()) {
            RSocketExchange exchange = new RSocketExchange(FrameType.REQUEST_RESPONSE, gsvRoutingMetadata, payload, this.upstreamBrokerMetadata);
            destination = filterChain.filter(exchange).then(destination);
        }
        //call destination
        return destination.flatMap(rsocket -> {
            metrics(gsvRoutingMetadata, "0x05");
            return rsocket.requestResponse(payload);
        });
    }

    @Override
    public @NotNull Flux<Payload> requestStream(@NotNull Payload payload) {
        BinaryRoutingMetadata binaryRoutingMetadata = binaryRoutingMetadata(payload.metadata());
        GSVRoutingMetadata gsvRoutingMetadata;
        if (binaryRoutingMetadata != null) {
            gsvRoutingMetadata = GSVRoutingMetadata.from(new String(binaryRoutingMetadata.getRoutingText(), StandardCharsets.UTF_8));
        } else {
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
            gsvRoutingMetadata = compositeMetadata.getRoutingMetaData();
            if (gsvRoutingMetadata == null) {
                return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
            }
        }
        Mono<RSocket> destination = findDestination(gsvRoutingMetadata);
        if (this.filterChain.isFiltersPresent()) {
            RSocketExchange requestContext = new RSocketExchange(FrameType.REQUEST_STREAM, gsvRoutingMetadata, payload, this.upstreamBrokerMetadata);
            destination = filterChain.filter(requestContext).then(destination);
        }
        return destination.flatMapMany(rsocket -> {
            metrics(gsvRoutingMetadata, "0x06");
            return rsocket.requestStream(payload);
        });
    }

    @Override
    public @NotNull Flux<Payload> requestChannel(@NotNull Publisher<Payload> payloads) {
        if (payloads instanceof Flux) {
            Flux<Payload> payloadsWithSignalRouting = (Flux<Payload>) payloads;
            //noinspection ConstantConditions
            return payloadsWithSignalRouting.switchOnFirst((signal, flux) -> requestChannel(signal.get(), flux));
        }
        return Flux.error(new InvalidException(RsocketErrorCode.message("RST-201400")));
    }


    public Flux<Payload> requestChannel(Payload signal, Publisher<Payload> payloads) {
        BinaryRoutingMetadata binaryRoutingMetadata = binaryRoutingMetadata(signal.metadata());
        GSVRoutingMetadata gsvRoutingMetadata;
        if (binaryRoutingMetadata != null) {
            gsvRoutingMetadata = GSVRoutingMetadata.from(new String(binaryRoutingMetadata.getRoutingText(), StandardCharsets.UTF_8));
        } else {
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(signal.metadata());
            gsvRoutingMetadata = compositeMetadata.getRoutingMetaData();
            if (gsvRoutingMetadata == null) {
                return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
            }
        }
        Mono<RSocket> destination = findDestination(gsvRoutingMetadata);
        return destination.flatMapMany(rsocket -> {
            metrics(gsvRoutingMetadata, "0x07");
            return rsocket.requestChannel(payloads);
        });
    }

    @Override
    public @NotNull Mono<Void> metadataPush(@NotNull Payload payload) {
        try {
            if (payload.metadata().readableBytes() > 0) {
                CloudEventImpl<JsonNode> cloudEvent = Json.decodeValue(payload.getMetadataUtf8());
                String type = cloudEvent.getAttributes().getType();
                if (UpstreamClusterChangedEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
                    handleUpstreamClusterChangedEvent(cloudEvent);
                }
            }
        } catch (Exception e) {
            log.error(RsocketErrorCode.message(RsocketErrorCode.message("RST-610500", e.getMessage())), e);
        } finally {
            ReferenceCountUtil.safeRelease(payload);
        }
        return Mono.empty();
    }


    @Nullable
    protected BinaryRoutingMetadata binaryRoutingMetadata(ByteBuf compositeByteBuf) {
        long typeAndService = compositeByteBuf.getLong(0);
        if ((typeAndService >> 56) == BINARY_ROUTING_MARK) {
            int metadataContentLen = (int) (typeAndService >> 32) & 0x00FFFFFF;
            return BinaryRoutingMetadata.from(compositeByteBuf.slice(4, metadataContentLen));
        }
        return null;
    }

    private Mono<RSocket> findDestination(GSVRoutingMetadata routingMetaData) {
        return Mono.create(sink -> {
            String gsv = routingMetaData.gsv();
            Integer serviceId = routingMetaData.id();
            RSocketBrokerResponderHandler targetHandler = null;
            RSocket rsocket = null;
            Exception error = null;
            String endpoint = routingMetaData.getEndpoint();
            if (endpoint != null && !endpoint.isEmpty()) {
                targetHandler = findDestinationWithEndpoint(endpoint, serviceId);
                if (targetHandler == null) {
                    error = new InvalidException(RsocketErrorCode.message("RST-900405", gsv, endpoint));
                }
            } else {
                Integer targetHandlerId = routingSelector.findHandler(serviceId);
                if (targetHandlerId != null) {
                    targetHandler = handlerRegistry.findById(targetHandlerId);
                } else {
                    error = new InvalidException(RsocketErrorCode.message("RST-900404", gsv));
                }
            }
            //security check
            if (targetHandler != null) {
                rsocket = targetHandler.getPeerRsocket();
                   /* if (serviceMeshInspector.isRequestAllowed(this.principal, gsv, targetHandler.principal)) {
                        rsocket = targetHandler.getPeerRsocket();
                    } else {
                        error = new ApplicationErrorException(RsocketErrorCode.message("RST-900401", gsv));
                    }*/
            }
            if (rsocket != null) {
                sink.success(rsocket);
            } else if (error != null) {
                sink.error(error);
            } else {
                sink.error(new ApplicationErrorException(RsocketErrorCode.message("RST-900404", gsv)));
            }
        });
    }

    private RSocketBrokerResponderHandler findDestinationWithEndpoint(String endpoint, Integer serviceId) {
        if (endpoint.startsWith("id:")) {
            return handlerRegistry.findByUUID(endpoint.substring(3));
        }
        int endpointHashCode = endpoint.hashCode();
        for (Integer handlerId : routingSelector.findHandlers(serviceId)) {
            RSocketBrokerResponderHandler handler = handlerRegistry.findById(handlerId);
            if (handler != null) {
                if (handler.getAppTagsHashCodeSet().contains(endpointHashCode)) {
                    return handler;
                }
            }
        }
        return null;
    }

    public void handleUpstreamClusterChangedEvent(CloudEventImpl<?> cloudEvent) {
        UpstreamClusterChangedEvent clusterChangedEvent = CloudEventSupport.unwrapData(cloudEvent, UpstreamClusterChangedEvent.class);
        if (clusterChangedEvent != null) {
            upstreamBrokerCluster.setUris(clusterChangedEvent.getUris());
            log.info(RsocketErrorCode.message("RST-300202", "UpstreamBroker", String.join(",", clusterChangedEvent.getUris())));
        }
    }
}
