package com.alibaba.spring.boot.rsocket.broker.responder;

import com.alibaba.rsocket.RSocketExchange;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventRSocket;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.listen.RSocketResponderSupport;
import com.alibaba.rsocket.metadata.*;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.route.RSocketFilterChain;
import com.alibaba.rsocket.route.RSocketRequestType;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceMeshInspector;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.alibaba.spring.boot.rsocket.broker.security.RSocketAppPrincipal;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cloudevents.json.Json;
import io.cloudevents.v1.CloudEventImpl;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.ResponderRSocket;
import io.rsocket.exceptions.ApplicationErrorException;
import io.rsocket.exceptions.InvalidException;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.util.ByteBufPayload;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.extra.processor.TopicProcessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RSocket broker responder handler for per connection
 *
 * @author leijuan
 */
@SuppressWarnings({"Duplicates", "rawtypes"})
public class RSocketBrokerResponderHandler extends RSocketResponderSupport implements ResponderRSocket, CloudEventRSocket {
    private static Logger log = LoggerFactory.getLogger(RSocketBrokerResponderHandler.class);
    /**
     * rsocket filter for requests
     */
    private RSocketFilterChain filterChain;
    /**
     * default message mime type metadata
     */
    private MessageMimeTypeMetadata defaultMessageMimeType;
    /**
     * app metadata
     */
    private AppMetadata appMetadata;
    /**
     * authorized principal
     */
    private RSocketAppPrincipal principal;
    /**
     * peer services
     */
    private Set<ServiceLocator> peerServices;
    /**
     * consumed service
     */
    private Set<String> consumedServices = new HashSet<>();
    /**
     * peer RSocket: sending or requester RSocket
     */
    private RSocket peerRsocket;
    /**
     * app status: 0:connect, 1: serving, 2: not serving  -1: stopped
     */
    private Integer appStatus = AppStatusEvent.STATUS_CONNECTED;
    /**
     * reactive service routing selector
     */
    private ServiceRoutingSelector routingSelector;
    private RSocketBrokerHandlerRegistry handlerRegistry;
    private ServiceMeshInspector serviceMeshInspector;
    private Mono<Void> comboOnClose;
    /**
     * reactive event processor
     */
    private TopicProcessor<CloudEventImpl> eventProcessor;
    /**
     * UUID from requester side
     */
    private String uuid;
    /**
     * app instance id
     */
    private Integer id;

    public RSocketBrokerResponderHandler(ConnectionSetupPayload setupPayload,
                                         RSocketCompositeMetadata compositeMetadata,
                                         AppMetadata appMetadata,
                                         RSocketAppPrincipal principal,
                                         RSocket peerRsocket,
                                         ServiceRoutingSelector routingSelector,
                                         TopicProcessor<CloudEventImpl> eventProcessor,
                                         RSocketBrokerHandlerRegistry handlerRegistry,
                                         ServiceMeshInspector serviceMeshInspector) {
        try {
            RSocketMimeType dataType = RSocketMimeType.valueOfType(setupPayload.dataMimeType());
            if (dataType != null) {
                this.defaultMessageMimeType = new MessageMimeTypeMetadata(dataType);
            }
            this.id = appMetadata.getId();
            this.appMetadata = appMetadata;
            this.uuid = this.appMetadata.getUuid();
            this.principal = principal;
            this.peerRsocket = peerRsocket;
            this.routingSelector = routingSelector;
            this.eventProcessor = eventProcessor;
            this.handlerRegistry = handlerRegistry;
            this.serviceMeshInspector = serviceMeshInspector;
            //publish services metadata
            if (compositeMetadata.contains(RSocketMimeType.ServiceRegistry)) {
                ServiceRegistryMetadata serviceRegistryMetadata = ServiceRegistryMetadata.from(compositeMetadata.getMetadata(RSocketMimeType.ServiceRegistry));
                if (serviceRegistryMetadata.getPublished() != null && !serviceRegistryMetadata.getPublished().isEmpty()) {
                    setPeerServices(serviceRegistryMetadata.getPublished());
                    registerPublishedServices();
                }
            }
            //new comboOnClose
            this.comboOnClose = Mono.first(super.onClose(), peerRsocket.onClose());
            this.comboOnClose.doOnTerminate(this::unRegisterPublishedServices).subscribeOn(Schedulers.parallel()).subscribe();
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-500400"), e);
        }
    }

    public String getUuid() {
        return this.uuid;
    }

    public Integer getId() {
        return id;
    }

    public AppMetadata getAppMetadata() {
        return appMetadata;
    }

    public Set<String> getConsumedServices() {
        return consumedServices;
    }

    public Integer getAppStatus() {
        return appStatus;
    }

    public void setAppStatus(Integer appStatus) {
        this.appStatus = appStatus;
    }

    public void setFilterChain(RSocketFilterChain rsocketFilterChain) {
        this.filterChain = rsocketFilterChain;
    }

    public void setLocalReactiveServiceCaller(LocalReactiveServiceCaller localReactiveServiceCaller) {
        this.localServiceCaller = localReactiveServiceCaller;
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
        GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
        if (routingMetaData == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
        }
        // broker local service call check: don't introduce interceptor, performance consideration
        if (localServiceCaller.contains(routingMetaData.getService())) {
            MessageMimeTypeMetadata dataEncodingMetadata = compositeMetadata.getDataEncodingMetadata();
            if (dataEncodingMetadata == null) {
                dataEncodingMetadata = defaultMessageMimeType;
            }
            return localRequestResponse(routingMetaData, dataEncodingMetadata, compositeMetadata.getAcceptMimeTypesMetadata(), payload);
        }
        metrics(routingMetaData, "0x05");
        String routing = routingMetaData.routing();
        //payload exchange
        RSocket destination = findDestination(routingMetaData, routing);
        if (destination != null) {
            recordServiceInvoke(principal.getName(), routing);
            //request filters
            if (this.filterChain.isFiltersPresent()) {
                RSocketExchange exchange = new RSocketExchange(RSocketRequestType.REQUEST_RESPONSE, routingMetaData, payload);
                return filterChain.filter(exchange).then(destination.requestResponse(payloadWithDataEncoding(compositeMetadata, payload)));
            }
            return destination.requestResponse(payloadWithDataEncoding(compositeMetadata, payload));
        }
        return Mono.error(new ApplicationErrorException(RsocketErrorCode.message("RST-900404", routing)));
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
        GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
        if (routingMetaData == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
        }
        // broker local service call check: don't introduce interceptor, performance consideration
        if (localServiceCaller.contains(routingMetaData.getService())) {
            MessageMimeTypeMetadata dataEncodingMetadata = compositeMetadata.getDataEncodingMetadata();
            if (dataEncodingMetadata == null) {
                dataEncodingMetadata = defaultMessageMimeType;
            }
            return localFireAndForget(routingMetaData, dataEncodingMetadata, payload);
        }
        metrics(routingMetaData, "0x05");
        String routing = routingMetaData.routing();
        RSocket destination = findDestination(routingMetaData, routing);
        if (destination != null) {
            recordServiceInvoke(principal.getName(), routing);
            if (this.filterChain.isFiltersPresent()) {
                RSocketExchange requestContext = new RSocketExchange(RSocketRequestType.REQUEST_RESPONSE, routingMetaData, payload);
                return filterChain.filter(requestContext).then(destination.fireAndForget(payloadWithDataEncoding(compositeMetadata, payload)));
            }
            return destination.fireAndForget(payloadWithDataEncoding(compositeMetadata, payload));
        }
        return Mono.error(new ApplicationErrorException(RsocketErrorCode.message("RST-900404", routing)));
    }


    @Override
    public Mono<Void> fireCloudEvent(CloudEventImpl<?> cloudEvent) {
        //要进行event的安全验证，不合法来源的event进行消费，后续还好进行event判断
        if (uuid.equalsIgnoreCase(cloudEvent.getAttributes().getSource().getHost())) {
            return Mono.fromRunnable(() -> eventProcessor.onNext(cloudEvent));
        }
        return Mono.empty();
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
        GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
        if (routingMetaData == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
        }
        // broker local service call check: don't introduce interceptor, performance consideration
        if (localServiceCaller.contains(routingMetaData.getService())) {
            MessageMimeTypeMetadata dataEncodingMetadata = compositeMetadata.getDataEncodingMetadata();
            if (dataEncodingMetadata == null) {
                dataEncodingMetadata = defaultMessageMimeType;
            }
            return localRequestStream(routingMetaData, dataEncodingMetadata, compositeMetadata.getAcceptMimeTypesMetadata(), payload);
        }
        metrics(routingMetaData, "0x06");
        String routing = routingMetaData.routing();
        RSocket destination = findDestination(routingMetaData, routing);
        if (destination != null) {
            recordServiceInvoke(principal.getName(), routing);
            if (this.filterChain.isFiltersPresent()) {
                RSocketExchange requestContext = new RSocketExchange(RSocketRequestType.REQUEST_RESPONSE, routingMetaData, payload);
                return filterChain.filter(requestContext).thenMany(destination.requestStream(payloadWithDataEncoding(compositeMetadata, payload)));
            }
            return destination.requestStream(payloadWithDataEncoding(compositeMetadata, payload));
        }
        return Flux.error(new ApplicationErrorException(RsocketErrorCode.message("RST-900404", routing)));
    }

    @Override
    public Flux<Payload> requestChannel(Payload signal, Publisher<Payload> payloads) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(signal.metadata());
        GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
        if (routingMetaData == null) {
            ReferenceCountUtil.safeRelease(signal);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
        }
        metrics(routingMetaData, "0x07");
        String routing = routingMetaData.routing();
        RSocket destination = findDestination(routingMetaData, routing);
        if (destination != null) {
            recordServiceInvoke(principal.getName(), routing);
            return destination.requestChannel(payloads);
        }
        return Flux.error(new ApplicationErrorException(RsocketErrorCode.message("RST-900404", routing)));
    }

    @Override
    public Mono<Void> metadataPush(Payload payload) {
        try {
            if (payload.metadata().capacity() > 0) {
                CloudEventImpl<ObjectNode> cloudEvent = Json.decodeValue(payload.getMetadataUtf8(), CLOUD_EVENT_TYPE_REFERENCE);
                return fireCloudEvent(cloudEvent);
            }
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-610500", e.getMessage()), e);
        } finally {
            ReferenceCountUtil.safeRelease(payload);
        }
        return Mono.empty();
    }

    public void setPeerServices(Set<ServiceLocator> services) {
        this.peerServices = services;
    }

    public void registerPublishedServices() {
        if (this.peerServices != null && !this.peerServices.isEmpty()) {
            routingSelector.register(appMetadata.getId(), peerServices.stream().map(ServiceLocator::routing).collect(Collectors.toSet()));
        }
        this.appStatus = AppStatusEvent.STATUS_SERVING;
    }

    public void unRegisterPublishedServices() {
        routingSelector.deregister(appMetadata.getId());
        this.appStatus = AppStatusEvent.STATUS_OUT_OF_SERVICE;
    }

    public Set<ServiceLocator> getPeerServices() {
        return peerServices;
    }


    public Mono<Void> fireCloudEventToPeer(CloudEventImpl<?> cloudEvent) {
        try {
            Payload payload = cloudEventToMetadataPushPayload(cloudEvent);
            return peerRsocket.metadataPush(payload);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private void recordServiceInvoke(String name, String serviceId) {
        consumedServices.add(serviceId);
    }

    /**
     * get role: 0bxx
     *
     * @return role
     */
    public Integer getRoles() {
        int role = 0;
        if (consumedServices != null && !consumedServices.isEmpty()) {
            role = role + 1;
        }
        if (peerServices != null && !peerServices.isEmpty()) {
            role = role + 2;
        }
        return role;
    }

    /**
     * payload with data encoding if
     *
     * @param compositeMetadata composite metadata
     * @param payload           payload
     * @return payload
     */
    public Payload payloadWithDataEncoding(RSocketCompositeMetadata compositeMetadata, Payload payload) {
        if (compositeMetadata.contains(RSocketMimeType.MessageMimeType)) {
            return payload;
        } else {
            ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer(5);
            buf.writeByte((byte) (WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getIdentifier() | 0x80));
            buf.writeByte(0);
            buf.writeByte(0);
            buf.writeByte(1);
            buf.writeByte(defaultMessageMimeType.getRSocketMimeType().getId() | 0x80);
            CompositeByteBuf compositeByteBuf = new CompositeByteBuf(PooledByteBufAllocator.DEFAULT, true, 3, payload.metadata(), buf);
            return ByteBufPayload.create(payload.data(), compositeByteBuf);
        }
    }

    @Nullable
    private RSocket findDestination(GSVRoutingMetadata routingMetaData, String routing) {
        Integer serviceId = routingMetaData.id();
        Integer handlerId;
        if (routingMetaData.getEndpoint() != null && !routingMetaData.getEndpoint().isEmpty()) {
            handlerId = findDestinationWithEndpoint(routingMetaData.getEndpoint(), serviceId);
        } else {
            handlerId = routingSelector.findHandler(serviceId);
        }
        if (handlerId != null) {
            RSocketBrokerResponderHandler targetHandler = handlerRegistry.findById(handlerId);
            if (targetHandler != null) {
                if (serviceMeshInspector.isRequestAllowed(this.principal, routing, targetHandler.principal)) {
                    return targetHandler.peerRsocket;
                }
            }
        }
        return null;
    }

    @Nullable
    private Integer findDestinationWithEndpoint(String endpoint, Integer serviceId) {
        int sepPosition = endpoint.indexOf(':');
        String key = endpoint.substring(0, sepPosition);
        String value = endpoint.substring(sepPosition + 1);
        for (Integer handlerId : routingSelector.findHandlers(serviceId)) {
            RSocketBrokerResponderHandler handler = handlerRegistry.findById(handlerId);
            if (handler != null) {
                if (key.equalsIgnoreCase("ip") && value.equalsIgnoreCase(handler.getAppMetadata().getIp())) {
                    return handlerId;
                } else if (endpoint.equalsIgnoreCase("id") && value.equalsIgnoreCase(handler.getAppMetadata().getUuid())) {
                    return handlerId;
                } else if (appMetadata.getMetadata() != null && value.equalsIgnoreCase(appMetadata.getMetadata(key))) {
                    return handlerId;
                }
            }
        }
        return null;
    }

    @Override
    public Mono<Void> onClose() {
        return this.comboOnClose;
    }

    private static void metrics(GSVRoutingMetadata routing, String frameType) {
        List<Tag> tags = new ArrayList<>();
        if (routing.getGroup() != null && !routing.getGroup().isEmpty()) {
            tags.add(Tag.of("group", routing.getGroup()));
        }
        if (routing.getVersion() != null && !routing.getVersion().isEmpty()) {
            tags.add(Tag.of("version", routing.getVersion()));
        }
        tags.add(Tag.of("method", routing.getMethod()));
        tags.add(Tag.of("frame", frameType));
        Metrics.counter(routing.getService(), tags).increment();
        Metrics.counter("rsocket.request.count").increment();
        Metrics.counter(routing.getService() + ".all").increment();
    }
}
