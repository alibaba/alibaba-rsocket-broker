package com.alibaba.spring.boot.rsocket.broker.responder;

import com.alibaba.rsocket.RSocketExchange;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.CloudEventRSocket;
import com.alibaba.rsocket.cloudevents.EventReply;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.listen.RSocketResponderSupport;
import com.alibaba.rsocket.metadata.*;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.route.RSocketFilterChain;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceMeshInspector;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.alibaba.spring.boot.rsocket.broker.security.RSocketAppPrincipal;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.DuplexConnection;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.exceptions.ApplicationErrorException;
import io.rsocket.exceptions.InvalidException;
import io.rsocket.frame.FrameType;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.util.ByteBufPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * RSocket broker responder handler for per connection
 *
 * @author leijuan
 */
@SuppressWarnings({"Duplicates", "rawtypes"})
public class RSocketBrokerResponderHandler extends RSocketResponderSupport implements CloudEventRSocket {
    private static Logger log = LoggerFactory.getLogger(RSocketBrokerResponderHandler.class);
    /**
     * binary routing mark
     */
    private static final byte BINARY_ROUTING_MARK = (byte) (WellKnownMimeType.MESSAGE_RSOCKET_BINARY_ROUTING.getIdentifier() | 0x80);
    /**
     * default timeout, and unit is second.  Make sure to clean the queue
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    /**
     * rsocket filter for requests
     */
    private RSocketFilterChain filterChain;
    /**
     * default message mime type metadata
     */
    private MessageMimeTypeMetadata defaultMessageMimeType;
    /**
     * default data encoding bytebuf
     */
    private ByteBuf defaultEncodingBytebuf;
    /**
     * app metadata
     */
    private AppMetadata appMetadata;
    /**
     * app tags hashcode hash set, to make routing based on endpoint fast
     */
    private Set<Integer> appTagsHashCodeSet = new HashSet<>();
    /**
     * authorized principal
     */
    private RSocketAppPrincipal principal;
    /**
     * peer services
     */
    private Set<ServiceLocator> peerServices;
    /**
     * sticky services, value is rsocket handler id
     */
    private Map<Integer, Integer> stickyServices = new HashMap<>();
    /**
     * consumed service
     */
    private Set<String> consumedServices = new HashSet<>();
    /**
     * peer RSocket: sending or requester RSocket
     */
    private RSocket peerRsocket;
    @Nullable
    private RSocket upstreamRSocket;
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
    private Sinks.Many<CloudEventImpl> eventProcessor;
    /**
     * UUID from requester side
     */
    private String uuid;
    /**
     * remote requester ip
     */
    private String remoteIp;
    /**
     * app instance id
     */
    private Integer id;

    public RSocketBrokerResponderHandler(@NotNull ConnectionSetupPayload setupPayload,
                                         @NotNull RSocketCompositeMetadata compositeMetadata,
                                         @NotNull AppMetadata appMetadata,
                                         @NotNull RSocketAppPrincipal principal,
                                         RSocket peerRsocket,
                                         ServiceRoutingSelector routingSelector,
                                         Sinks.Many<CloudEventImpl> eventProcessor,
                                         RSocketBrokerHandlerRegistry handlerRegistry,
                                         ServiceMeshInspector serviceMeshInspector,
                                         @Nullable RSocket upstreamRSocket) {
        try {
            this.upstreamRSocket = upstreamRSocket;
            RSocketMimeType dataType = RSocketMimeType.valueOfType(setupPayload.dataMimeType());
            if (dataType != null) {
                this.defaultMessageMimeType = new MessageMimeTypeMetadata(dataType);
                this.defaultEncodingBytebuf = constructDefaultDataEncoding();
            }
            this.appMetadata = appMetadata;
            this.id = appMetadata.getId();
            this.uuid = appMetadata.getUuid();
            //app tags hashcode set
            this.appTagsHashCodeSet.add(("id:" + this.id).hashCode());
            this.appTagsHashCodeSet.add(("uuid:" + this.uuid).hashCode());
            if (appMetadata.getIp() != null && !appMetadata.getIp().isEmpty()) {
                this.appTagsHashCodeSet.add(("ip:" + this.appMetadata.getIp()).hashCode());
            }
            if (appMetadata.getMetadata() != null) {
                for (Map.Entry<String, String> entry : appMetadata.getMetadata().entrySet()) {
                    this.appTagsHashCodeSet.add((entry.getKey() + ":" + entry.getValue()).hashCode());
                }
            }
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
            //remote ip
            this.remoteIp = getRemoteAddress(peerRsocket);
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

    @Nullable
    public String getRemoteIp() {
        return remoteIp;
    }

    public AppMetadata getAppMetadata() {
        return appMetadata;
    }

    public Set<String> getConsumedServices() {
        return consumedServices;
    }

    public Set<Integer> getAppTagsHashCodeSet() {
        return appTagsHashCodeSet;
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
    @NotNull
    public Mono<Payload> requestResponse(Payload payload) {
        BinaryRoutingMetadata binaryRoutingMetadata = binaryRoutingMetadata(payload.metadata());
        GSVRoutingMetadata gsvRoutingMetadata;
        Integer serviceId;
        final boolean encodingMetadataIncluded;
        if (binaryRoutingMetadata != null) {
            gsvRoutingMetadata = GSVRoutingMetadata.from(new String(binaryRoutingMetadata.getRoutingText(), StandardCharsets.UTF_8));
            serviceId = binaryRoutingMetadata.getServiceId();
            encodingMetadataIncluded = true;
        } else {
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
            gsvRoutingMetadata = compositeMetadata.getRoutingMetaData();
            if (gsvRoutingMetadata == null) {
                return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
            }
            encodingMetadataIncluded = compositeMetadata.contains(RSocketMimeType.MessageMimeType);
            serviceId = gsvRoutingMetadata.id();
        }
        // broker local service call check: don't introduce interceptor, performance consideration
        if (localServiceCaller.contains(serviceId)) {
            return localRequestResponse(gsvRoutingMetadata, defaultMessageMimeType, null, payload);
        }
        //request filters
        Mono<RSocket> destination = findDestination(binaryRoutingMetadata, gsvRoutingMetadata);
        if (this.filterChain.isFiltersPresent()) {
            RSocketExchange exchange = new RSocketExchange(FrameType.REQUEST_RESPONSE, gsvRoutingMetadata, payload, this.appMetadata);
            destination = filterChain.filter(exchange).then(destination);
        }
        //call destination
        return destination.flatMap(rsocket -> {
            recordServiceInvoke(principal.getName(), gsvRoutingMetadata.gsv());
            metrics(gsvRoutingMetadata, "0x05");
            if (encodingMetadataIncluded) {
                return rsocket.requestResponse(payload);
            } else {
                return rsocket.requestResponse(payloadWithDataEncoding(payload));
            }
        });
    }

    @Override
    @NotNull
    public Mono<Void> fireAndForget(Payload payload) {
        BinaryRoutingMetadata binaryRoutingMetadata = binaryRoutingMetadata(payload.metadata());
        GSVRoutingMetadata gsvRoutingMetadata;
        Integer serviceId;
        final boolean encodingMetadataIncluded;
        if (binaryRoutingMetadata != null) {
            gsvRoutingMetadata = GSVRoutingMetadata.from(new String(binaryRoutingMetadata.getRoutingText(), StandardCharsets.UTF_8));
            serviceId = binaryRoutingMetadata.getServiceId();
            encodingMetadataIncluded = true;
        } else {
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
            gsvRoutingMetadata = compositeMetadata.getRoutingMetaData();
            if (gsvRoutingMetadata == null) {
                return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
            }
            encodingMetadataIncluded = compositeMetadata.contains(RSocketMimeType.MessageMimeType);
            serviceId = gsvRoutingMetadata.id();
        }
        if (localServiceCaller.contains(serviceId)) {
            return localFireAndForget(gsvRoutingMetadata, defaultMessageMimeType, payload);
        }
        //request filters
        Mono<RSocket> destination = findDestination(binaryRoutingMetadata, gsvRoutingMetadata);
        if (this.filterChain.isFiltersPresent()) {
            RSocketExchange exchange = new RSocketExchange(FrameType.REQUEST_FNF, gsvRoutingMetadata, payload, this.appMetadata);
            destination = filterChain.filter(exchange).then(destination);
        }
        //call destination
        return destination.flatMap(rsocket -> {
            recordServiceInvoke(principal.getName(), gsvRoutingMetadata.gsv());
            metrics(gsvRoutingMetadata, "0x05");
            if (encodingMetadataIncluded) {
                return rsocket.fireAndForget(payload);
            } else {
                return rsocket.fireAndForget(payloadWithDataEncoding(payload));
            }
        });
    }


    @Override
    public Mono<Void> fireCloudEvent(CloudEventImpl<?> cloudEvent) {
        //要进行event的安全验证，不合法来源的event进行消费，后续还好进行event判断
        if (uuid.equalsIgnoreCase(cloudEvent.getAttributes().getSource().getHost())) {
            return Mono.fromRunnable(() -> eventProcessor.tryEmitNext(cloudEvent));
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> fireEventReply(URI replayTo, EventReply eventReply) {
        return peerRsocket.fireAndForget(constructEventReplyPayload(replayTo, eventReply));
    }

    @Override
    @NotNull
    public Flux<Payload> requestStream(Payload payload) {
        BinaryRoutingMetadata binaryRoutingMetadata = binaryRoutingMetadata(payload.metadata());
        GSVRoutingMetadata gsvRoutingMetadata;
        Integer serviceId;
        final boolean encodingMetadataIncluded;
        if (binaryRoutingMetadata != null) {
            gsvRoutingMetadata = GSVRoutingMetadata.from(new String(binaryRoutingMetadata.getRoutingText(), StandardCharsets.UTF_8));
            serviceId = binaryRoutingMetadata.getServiceId();
            encodingMetadataIncluded = true;
        } else {
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
            gsvRoutingMetadata = compositeMetadata.getRoutingMetaData();
            if (gsvRoutingMetadata == null) {
                return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
            }
            encodingMetadataIncluded = compositeMetadata.contains(RSocketMimeType.MessageMimeType);
            serviceId = gsvRoutingMetadata.id();
        }
        // broker local service call check: don't introduce interceptor, performance consideration
        if (localServiceCaller.contains(serviceId)) {
            return localRequestStream(gsvRoutingMetadata, defaultMessageMimeType, null, payload);
        }
        Mono<RSocket> destination = findDestination(binaryRoutingMetadata, gsvRoutingMetadata);
        if (this.filterChain.isFiltersPresent()) {
            RSocketExchange requestContext = new RSocketExchange(FrameType.REQUEST_STREAM, gsvRoutingMetadata, payload, this.appMetadata);
            destination = filterChain.filter(requestContext).then(destination);
        }
        return destination.flatMapMany(rsocket -> {
            recordServiceInvoke(principal.getName(), gsvRoutingMetadata.gsv());
            metrics(gsvRoutingMetadata, "0x06");
            if (encodingMetadataIncluded) {
                return rsocket.requestStream(payload);
            } else {
                return rsocket.requestStream(payloadWithDataEncoding(payload));
            }
        });
    }

    public Flux<Payload> requestChannel(Payload signal, Flux<Payload> payloads) {
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
        Mono<RSocket> destination = findDestination(binaryRoutingMetadata, gsvRoutingMetadata);
        return destination.flatMapMany(rsocket -> {
            recordServiceInvoke(principal.getName(), gsvRoutingMetadata.gsv());
            metrics(gsvRoutingMetadata, "0x07");
            return rsocket.requestChannel(payloads);
        });
    }

    @Override
    @NotNull
    public Flux<Payload> requestChannel(@NotNull Publisher<Payload> payloads) {
        if (payloads instanceof Flux) {
            Flux<Payload> payloadsWithSignalRouting = (Flux<Payload>) payloads;
            //noinspection ConstantConditions
            return payloadsWithSignalRouting.switchOnFirst((signal, flux) -> requestChannel(signal.get(), flux));
        }
        return Flux.error(new InvalidException(RsocketErrorCode.message("RST-201400")));
    }

    @Override
    @NotNull
    public Mono<Void> metadataPush(@NotNull Payload payload) {
        try {
            if (payload.metadata().readableBytes() > 0) {
                CloudEventImpl<?> cloudEvent = extractCloudEventsFromMetadataPush(payload);
                if (cloudEvent != null) {
                    return fireCloudEvent(cloudEvent);
                }
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
            Set<Integer> services = routingSelector.findServicesByInstance(appMetadata.getId());
            if (services.isEmpty()) {
                this.routingSelector.register(appMetadata.getId(), appMetadata.getPowerRating(), peerServices);
                this.appStatus = AppStatusEvent.STATUS_SERVING;
            }
        }
    }

    public void registerServices(Set<ServiceLocator> services) {
        if (this.peerServices == null || this.peerServices.isEmpty()) {
            this.peerServices = services;
        } else {
            this.peerServices.addAll(services);
        }
        this.routingSelector.register(appMetadata.getId(), appMetadata.getPowerRating(), services);
    }

    public void unRegisterPublishedServices() {
        routingSelector.deregister(appMetadata.getId());
        this.appStatus = AppStatusEvent.STATUS_OUT_OF_SERVICE;
    }

    public void unRegisterServices(Set<ServiceLocator> services) {
        if (this.peerServices != null && !this.peerServices.isEmpty()) {
            this.peerServices.removeAll(services);
        }
        for (ServiceLocator service : services) {
            this.routingSelector.deregister(appMetadata.getId(), service.getId());
        }
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

    public RSocketAppPrincipal getPrincipal() {
        return principal;
    }

    public RSocket getPeerRsocket() {
        return peerRsocket;
    }

    /**
     * payload with data encoding if
     *
     * @param payload payload
     * @return payload
     */
    public Payload payloadWithDataEncoding(Payload payload) {
        CompositeByteBuf compositeByteBuf = new CompositeByteBuf(PooledByteBufAllocator.DEFAULT, true, 2, payload.metadata(), this.defaultEncodingBytebuf.retainedDuplicate());
        return ByteBufPayload.create(payload.data(), compositeByteBuf);
    }

    private ByteBuf constructDefaultDataEncoding() {
        ByteBuf buf = Unpooled.buffer(5, 5);
        buf.writeByte((byte) (WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getIdentifier() | 0x80));
        buf.writeByte(0);
        buf.writeByte(0);
        buf.writeByte(1);
        buf.writeByte(defaultMessageMimeType.getRSocketMimeType().getId() | 0x80);
        return buf;
    }

    private Mono<RSocket> findDestination(@Nullable BinaryRoutingMetadata binaryRoutingMetadata, GSVRoutingMetadata routingMetaData) {
        return Mono.create(sink -> {
            String gsv = routingMetaData.gsv();
            Integer serviceId = routingMetaData.id();
            RSocket rsocket = null;
            Exception error = null;
            //sticky session handler
            boolean sticky = binaryRoutingMetadata != null ? binaryRoutingMetadata.isSticky() : routingMetaData.isSticky();
            RSocketBrokerResponderHandler targetHandler = findStickyHandler(sticky, serviceId);
            // handler from sticky services
            if (targetHandler != null) {
                rsocket = targetHandler.peerRsocket;
            } else {
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
                if (targetHandler != null) {
                    if (serviceMeshInspector.isRequestAllowed(this.principal, gsv, targetHandler.principal)) {
                        rsocket = targetHandler.peerRsocket;
                        //save handler id if sticky
                        if (sticky) {
                            this.stickyServices.put(serviceId, targetHandler.getId());
                        }
                    } else {
                        error = new ApplicationErrorException(RsocketErrorCode.message("RST-900401", gsv));
                    }
                }
            }
            if (rsocket != null) {
                sink.success(rsocket);
            } else if (error != null) {
                if (upstreamRSocket != null && error instanceof InvalidException) {
                    sink.success(upstreamRSocket);
                } else {
                    sink.error(error);
                }
            } else {
                sink.error(new ApplicationErrorException(RsocketErrorCode.message("RST-900404", gsv)));
            }
        });
    }

    @Nullable
    private RSocketBrokerResponderHandler findDestinationWithEndpoint(String endpoint, Integer serviceId) {
        if (endpoint.startsWith("id:")) {
            return handlerRegistry.findByUUID(endpoint.substring(3));
        }
        int endpointHashCode = endpoint.hashCode();
        for (Integer handlerId : routingSelector.findHandlers(serviceId)) {
            RSocketBrokerResponderHandler handler = handlerRegistry.findById(handlerId);
            if (handler != null) {
                if (handler.appTagsHashCodeSet.contains(endpointHashCode)) {
                    return handler;
                }
            }
        }
        return null;
    }

    @Override
    public @NotNull Mono<Void> onClose() {
        return this.comboOnClose;
    }

    public static void metrics(GSVRoutingMetadata routingMetadata, String frameType) {
        List<Tag> tags = new ArrayList<>();
        if (routingMetadata.getGroup() != null && !routingMetadata.getGroup().isEmpty()) {
            tags.add(Tag.of("group", routingMetadata.getGroup()));
        }
        if (routingMetadata.getVersion() != null && !routingMetadata.getVersion().isEmpty()) {
            tags.add(Tag.of("version", routingMetadata.getVersion()));
        }
        tags.add(Tag.of("method", routingMetadata.getMethod()));
        tags.add(Tag.of("frame", frameType));
        Metrics.counter(routingMetadata.getService(), tags).increment();
        Metrics.counter("rsocket.request.counter").increment();
        Metrics.counter(routingMetadata.getService() + ".counter").increment();
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

    @Nullable
    protected RSocketBrokerResponderHandler findStickyHandler(boolean sticky, Integer serviceId) {
        // todo 算法更新，如一致性hash算法，或者取余操作
        if (sticky && stickyServices.containsKey(serviceId)) {
            return handlerRegistry.findById(stickyServices.get(serviceId));
        }
        return null;
    }

    public void clean() {
        ReferenceCountUtil.release(this.defaultEncodingBytebuf);
    }

    private String getRemoteAddress(RSocket requesterSocket) {
        try {
            Field connectionField = ReflectionUtils.findField(requesterSocket.getClass(), "connection");
            if (connectionField != null) {
                DuplexConnection connection = (DuplexConnection) ReflectionUtils.getField(connectionField, requesterSocket);
                if (connection != null) {
                    SocketAddress remoteAddress = connection.remoteAddress();
                    if (remoteAddress instanceof InetSocketAddress) {
                        return ((InetSocketAddress) remoteAddress).getHostName();
                    }
                }
            }
        } catch (Exception ignore) {

        }
        return null;
    }
}
