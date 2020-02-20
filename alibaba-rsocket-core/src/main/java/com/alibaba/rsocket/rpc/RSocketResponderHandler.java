package com.alibaba.rsocket.rpc;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.cloudevents.CloudEventRSocket;
import com.alibaba.rsocket.listen.RSocketResponderSupport;
import com.alibaba.rsocket.metadata.*;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.cloudevents.json.Json;
import io.cloudevents.v1.CloudEventImpl;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.ResponderRSocket;
import io.rsocket.exceptions.InvalidException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.extra.processor.TopicProcessor;

import java.nio.charset.StandardCharsets;

/**
 * RSocket responder handler implementation, not singleton, per handler per connection
 *
 * @author leijuan
 */
@SuppressWarnings("Duplicates")
public class RSocketResponderHandler extends RSocketResponderSupport implements ResponderRSocket, CloudEventRSocket {
    /**
     * requester from peer
     */
    protected RSocket requester;
    protected MessageMimeTypeMetadata defaultMessageMimeType;
    /**
     * combo onClose from responder and requester
     */
    private Mono<Void> comboOnClose;
    protected TopicProcessor<CloudEventImpl> eventProcessor;

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
            //from remote
            if (!appMetadata.getUuid().equals(RSocketAppContext.ID)) {
                RSocketMimeType dataType = RSocketMimeType.valueOfType(setupPayload.dataMimeType());
                if (dataType != null) {
                    this.defaultMessageMimeType = new MessageMimeTypeMetadata(dataType);
                }
            }
        }
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
        BinaryRoutingMetadata binaryRoutingMetadata = compositeMetadata.getBinaryRoutingMetadata();
        GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
        if (binaryRoutingMetadata == null && routingMetaData == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
        }
        if (binaryRoutingMetadata != null && routingMetaData == null) {
            routingMetaData = GSVRoutingMetadata.from(new String(binaryRoutingMetadata.getRoutingText(), StandardCharsets.UTF_8));
        }
        MessageMimeTypeMetadata dataEncodingMetadata = compositeMetadata.getDataEncodingMetadata();
        if (dataEncodingMetadata == null && this.defaultMessageMimeType != null) {
            dataEncodingMetadata = this.defaultMessageMimeType;
        }
        if (dataEncodingMetadata == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-700404")));
        }
        return localRequestResponse(routingMetaData, dataEncodingMetadata, compositeMetadata.getAcceptMimeTypesMetadata(), payload);
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
        BinaryRoutingMetadata binaryRoutingMetadata = compositeMetadata.getBinaryRoutingMetadata();
        GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
        if (binaryRoutingMetadata == null && routingMetaData == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
        }
        if (binaryRoutingMetadata != null && routingMetaData == null) {
            routingMetaData = GSVRoutingMetadata.from(new String(binaryRoutingMetadata.getRoutingText(), StandardCharsets.UTF_8));
        }
        MessageMimeTypeMetadata dataEncodingMetadata = compositeMetadata.getDataEncodingMetadata();
        if (dataEncodingMetadata == null && this.defaultMessageMimeType != null) {
            dataEncodingMetadata = this.defaultMessageMimeType;
        }
        if (dataEncodingMetadata == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-700404")));
        }
        //normal fireAndForget
        return localFireAndForget(routingMetaData, dataEncodingMetadata, payload);
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
    public Flux<Payload> requestStream(Payload payload) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
        BinaryRoutingMetadata binaryRoutingMetadata = compositeMetadata.getBinaryRoutingMetadata();
        GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
        if (binaryRoutingMetadata == null && routingMetaData == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
        }
        if (binaryRoutingMetadata != null && routingMetaData == null) {
            routingMetaData = GSVRoutingMetadata.from(new String(binaryRoutingMetadata.getRoutingText(), StandardCharsets.UTF_8));
        }
        MessageMimeTypeMetadata dataEncodingMetadata = compositeMetadata.getDataEncodingMetadata();
        if (dataEncodingMetadata == null && this.defaultMessageMimeType != null) {
            dataEncodingMetadata = this.defaultMessageMimeType;
        }
        if (dataEncodingMetadata == null) {
            ReferenceCountUtil.safeRelease(payload);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-700404")));
        }
        return localRequestStream(routingMetaData, dataEncodingMetadata, compositeMetadata.getAcceptMimeTypesMetadata(), payload);
    }

    @Override
    public Flux<Payload> requestChannel(Payload signal, Publisher<Payload> payloads) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(signal.metadata());
        BinaryRoutingMetadata binaryRoutingMetadata = compositeMetadata.getBinaryRoutingMetadata();
        GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
        if (binaryRoutingMetadata == null && routingMetaData == null) {
            ReferenceCountUtil.safeRelease(signal);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
        }
        if (binaryRoutingMetadata != null && routingMetaData == null) {
            routingMetaData = GSVRoutingMetadata.from(new String(binaryRoutingMetadata.getRoutingText(), StandardCharsets.UTF_8));
        }
        MessageMimeTypeMetadata dataEncodingMetadata = compositeMetadata.getDataEncodingMetadata();
        if (dataEncodingMetadata == null) {
            ReferenceCountUtil.safeRelease(signal);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-700404")));
        }
        return localRequestChannel(routingMetaData, dataEncodingMetadata, compositeMetadata.getAcceptMimeTypesMetadata(), signal, Flux.from(payloads).skip(1));
    }

    /**
     * receive event from peer
     *
     * @param payload payload with metadata only
     * @return mono empty
     */
    @Override
    public Mono<Void> metadataPush(Payload payload) {
        try {
            if (payload.metadata().capacity() > 0) {
                return fireCloudEvent(Json.decodeValue(payload.getMetadataUtf8(), CLOUD_EVENT_TYPE_REFERENCE));
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
    public Mono<Void> onClose() {
        return this.comboOnClose;
    }
}
