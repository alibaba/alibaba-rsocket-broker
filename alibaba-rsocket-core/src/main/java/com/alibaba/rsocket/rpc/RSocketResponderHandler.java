package com.alibaba.rsocket.rpc;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.CloudEventRSocket;
import com.alibaba.rsocket.cloudevents.EventReply;
import com.alibaba.rsocket.listen.RSocketResponderSupport;
import com.alibaba.rsocket.metadata.*;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.exceptions.InvalidException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.extra.processor.TopicProcessor;
import reactor.util.context.Context;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * RSocket responder handler implementation, not singleton, per handler per connection
 *
 * @author leijuan
 */
@SuppressWarnings("Duplicates")
public class RSocketResponderHandler extends RSocketResponderSupport implements CloudEventRSocket {
    /**
     * requester from peer
     */
    protected RSocket requester;
    @Nullable
    protected MessageMimeTypeMetadata defaultMessageMimeType = null;
    /**
     * combo onClose from responder and requester
     */
    private Mono<Void> comboOnClose;
    protected TopicProcessor<CloudEventImpl> eventProcessor;
    private boolean braveTracing = true;
    private Tracer tracer;

    public RSocketResponderHandler(LocalReactiveServiceCaller serviceCall,
                                   TopicProcessor<CloudEventImpl> eventProcessor,
                                   RSocket requester,
                                   ConnectionSetupPayload setupPayload) {
        this.localServiceCaller = serviceCall;
        this.eventProcessor = eventProcessor;
        this.requester = requester;
        this.comboOnClose = Mono.first(super.onClose(), requester.onClose());
        //parse composite metadata
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(setupPayload.metadata());
        if (compositeMetadata.contains(RSocketMimeType.Application)) {
            AppMetadata appMetadata = AppMetadata.from(compositeMetadata.getMetadata(RSocketMimeType.Application));
            //from remote requester
            if (!appMetadata.getUuid().equals(RSocketAppContext.ID)) {
                RSocketMimeType dataType = RSocketMimeType.valueOfType(setupPayload.dataMimeType());
                if (dataType != null) {
                    this.defaultMessageMimeType = new MessageMimeTypeMetadata(dataType);
                }
            }
        }
        try {
            Class.forName("brave.propagation.TraceContext");
        } catch (ClassNotFoundException e) {
            this.braveTracing = false;
        }
    }

    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    @NotNull
    public Mono<Payload> requestResponse(Payload payload) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
        GSVRoutingMetadata routingMetaData = getGsvRoutingMetadata(compositeMetadata);
        if (routingMetaData == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
        }
        MessageMimeTypeMetadata dataEncodingMetadata = getDataEncodingMetadata(compositeMetadata);
        if (dataEncodingMetadata == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-700404")));
        }
        Mono<Payload> result = localRequestResponse(routingMetaData, dataEncodingMetadata, compositeMetadata.getAcceptMimeTypesMetadata(), payload);
        return injectTraceContext(result, compositeMetadata);
    }

    @Override
    @NotNull
    public Mono<Void> fireAndForget(Payload payload) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
        GSVRoutingMetadata routingMetaData = getGsvRoutingMetadata(compositeMetadata);
        if (routingMetaData == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
        }
        MessageMimeTypeMetadata dataEncodingMetadata = getDataEncodingMetadata(compositeMetadata);
        if (dataEncodingMetadata == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-700404")));
        }
        Mono<Void> result = localFireAndForget(routingMetaData, dataEncodingMetadata, payload);
        return injectTraceContext(result, compositeMetadata);
    }

    /**
     * receive cloud event from metadataPush
     *
     * @param cloudEvent cloud event
     * @return void
     */
    @Override
    public Mono<Void> fireCloudEvent(CloudEventImpl<?> cloudEvent) {
        return Mono.fromRunnable(() -> eventProcessor.onNext(cloudEvent));
    }

    @Override
    public Mono<Void> fireEventReply(URI replayTo, EventReply eventReply) {
        return requester.fireAndForget(constructEventReplyPayload(replayTo, eventReply));
    }

    @Override
    @NotNull
    public Flux<Payload> requestStream(Payload payload) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
        GSVRoutingMetadata routingMetaData = getGsvRoutingMetadata(compositeMetadata);
        if (routingMetaData == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
        }
        MessageMimeTypeMetadata dataEncodingMetadata = getDataEncodingMetadata(compositeMetadata);
        if (dataEncodingMetadata == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-700404")));
        }
        Flux<Payload> result = localRequestStream(routingMetaData, dataEncodingMetadata, compositeMetadata.getAcceptMimeTypesMetadata(), payload);
        return injectTraceContext(result, compositeMetadata);
    }

    public Flux<Payload> requestChannel(Payload signal, Publisher<Payload> payloads) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(signal.metadata());
        GSVRoutingMetadata routingMetaData = getGsvRoutingMetadata(compositeMetadata);
        if (routingMetaData == null) {
            ReferenceCountUtil.safeRelease(signal);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
        }
        MessageMimeTypeMetadata dataEncodingMetadata = getDataEncodingMetadata(compositeMetadata);
        if (dataEncodingMetadata == null) {
            ReferenceCountUtil.safeRelease(signal);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-700404")));
        }
        if (payloads instanceof Flux) {
            return localRequestChannel(routingMetaData, dataEncodingMetadata, compositeMetadata.getAcceptMimeTypesMetadata(), signal, ((Flux<Payload>) payloads).skip(1));
        } else {
            return localRequestChannel(routingMetaData, dataEncodingMetadata, compositeMetadata.getAcceptMimeTypesMetadata(), signal, Flux.from(payloads).skip(1));
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    @NotNull
    public final Flux<Payload> requestChannel(@NotNull Publisher<Payload> payloads) {
        if (payloads instanceof Flux) {
            Flux<Payload> payloadsWithSignalRouting = (Flux<Payload>) payloads;
            return payloadsWithSignalRouting.switchOnFirst((signal, flux) -> requestChannel(signal.get(), flux));
        }
        return Flux.error(new InvalidException(RsocketErrorCode.message("RST-201400")));
    }

    /**
     * receive event from peer
     *
     * @param payload payload with metadata only
     * @return mono empty
     */
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
            log.error(RsocketErrorCode.message(RsocketErrorCode.message("RST-610500", e.getMessage())), e);
        } finally {
            ReferenceCountUtil.safeRelease(payload);
        }
        return Mono.empty();
    }

    /**
     * fire cloud event to peer
     *
     * @param cloudEvent cloud event
     * @return void
     */
    public Mono<Void> fireCloudEventToPeer(CloudEventImpl<?> cloudEvent) {
        try {
            Payload payload = cloudEventToMetadataPushPayload(cloudEvent);
            return requester.metadataPush(payload);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @Override
    @NotNull
    public Mono<Void> onClose() {
        return this.comboOnClose;
    }

    @Nullable
    private MessageMimeTypeMetadata getDataEncodingMetadata(RSocketCompositeMetadata compositeMetadata) {
        MessageMimeTypeMetadata dataEncodingMetadata = compositeMetadata.getDataEncodingMetadata();
        if (dataEncodingMetadata == null) {
            return this.defaultMessageMimeType;
        } else {
            return dataEncodingMetadata;
        }
    }

    @Nullable
    private GSVRoutingMetadata getGsvRoutingMetadata(RSocketCompositeMetadata compositeMetadata) {
        BinaryRoutingMetadata binaryRoutingMetadata = compositeMetadata.getBinaryRoutingMetadata();
        GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
        if (binaryRoutingMetadata != null && routingMetaData == null) {
            routingMetaData = GSVRoutingMetadata.from(new String(binaryRoutingMetadata.getRoutingText(), StandardCharsets.UTF_8));
        }
        return routingMetaData;
    }

    @NotNull
    private TraceContext constructTraceContext(@NotNull TracingMetadata tracingMetadata) {
        return TraceContext.newBuilder()
                .parentId(tracingMetadata.parentId())
                .spanId(tracingMetadata.spanId())
                .traceIdHigh(tracingMetadata.traceIdHigh())
                .traceId(tracingMetadata.traceId())
                .build();
    }

    <T> Mono<T> injectTraceContext(Mono<T> payloadMono, RSocketCompositeMetadata compositeMetadata) {
        if (this.braveTracing && this.tracer != null) {
            TracingMetadata tracingMetadata = compositeMetadata.getTracingMetadata();
            if (tracingMetadata != null) {
                TraceContext traceContext = constructTraceContext(tracingMetadata);
                Span span = tracer.newChild(traceContext);
                return payloadMono
                        .doOnError(span::error)
                        .doOnSuccess(payload -> span.finish())
                        .subscriberContext(Context.of(TraceContext.class, traceContext));
            }
        }
        return payloadMono;
    }

    Flux<Payload> injectTraceContext(Flux<Payload> payloadFlux, RSocketCompositeMetadata compositeMetadata) {
        if (this.braveTracing && this.tracer != null) {
            TracingMetadata tracingMetadata = compositeMetadata.getTracingMetadata();
            if (tracingMetadata != null) {
                TraceContext traceContext = constructTraceContext(tracingMetadata);
                Span span = tracer.newChild(traceContext);
                return payloadFlux
                        .doOnError(span::error)
                        .doOnComplete(span::finish)
                        .subscriberContext(Context.of(TraceContext.class, traceContext));
            }
        }
        return payloadFlux;
    }
}
