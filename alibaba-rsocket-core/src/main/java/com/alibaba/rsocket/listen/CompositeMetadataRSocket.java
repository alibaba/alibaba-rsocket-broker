package com.alibaba.rsocket.listen;

import com.alibaba.rsocket.MutableContext;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.micrometer.core.instrument.Metrics;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.ResponderRSocket;
import io.rsocket.exceptions.InvalidException;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * rsocket with composite metadata parsing
 *
 * @author leijuan
 */
@SuppressWarnings("DuplicatedCode")
public class CompositeMetadataRSocket implements RSocket, ResponderRSocket {
    public static final String COMPOSITE_METADATA_KEY = "_CM";
    public static final String REAL_RSOCKET_HANDLER = "_RSH";
    private static Logger log = LoggerFactory.getLogger(CompositeMetadataRSocket.class);
    private RSocket source;

    public CompositeMetadataRSocket(RSocket source) {
        this.source = source;
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
        MutableContext reactiveContext = new MutableContext();
        try {
            //todo fast routing: 1 byte for routing mimetype, 3 bytes for routing metadata length, 1 byte routing key byte length, then read routing key
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
            GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
            if (routingMetaData == null) {
                ReferenceCountUtil.safeRelease(payload);
                return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
            }
            reactiveContext.put(COMPOSITE_METADATA_KEY, compositeMetadata);
            reactiveContext.put(REAL_RSOCKET_HANDLER, source);
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-600500", e.getMessage()), e);
            ReferenceCountUtil.safeRelease(payload);
            return Mono.empty();
        }
        return source.fireAndForget(payload).subscriberContext(reactiveContext);
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        MutableContext reactiveContext = new MutableContext();
        try {
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
            GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
            if (routingMetaData == null) {
                ReferenceCountUtil.safeRelease(payload);
                return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
            }
            reactiveContext.put(COMPOSITE_METADATA_KEY, compositeMetadata);
            reactiveContext.put(REAL_RSOCKET_HANDLER, source);
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-600500", e.getMessage()), e);
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-600500", e.getMessage())));
        }
        return source.requestResponse(payload).subscriberContext(reactiveContext);
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
        MutableContext reactiveContext = new MutableContext();
        try {
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
            GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
            if (routingMetaData == null) {
                ReferenceCountUtil.safeRelease(payload);
                return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
            }
            reactiveContext.put(COMPOSITE_METADATA_KEY, compositeMetadata);
            reactiveContext.put(REAL_RSOCKET_HANDLER, source);
        } catch (Exception e) {
            ReferenceCountUtil.safeRelease(payload);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600500", e.getMessage())));
        }
        return source.requestStream(payload).subscriberContext(reactiveContext);
    }

    @Override
    public Flux<Payload> requestChannel(Payload signal, Publisher<Payload> payloads) {
        MutableContext reactiveContext = new MutableContext();
        try {
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(signal.metadata());
            GSVRoutingMetadata routingMetaData = compositeMetadata.getRoutingMetaData();
            if (routingMetaData == null) {
                ReferenceCountUtil.safeRelease(signal);
                return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600404")));
            }
            reactiveContext.put(COMPOSITE_METADATA_KEY, compositeMetadata);
            reactiveContext.put(REAL_RSOCKET_HANDLER, source);
        } catch (Exception e) {
            ReferenceCountUtil.safeRelease(signal);
            return Flux.error(new InvalidException(RsocketErrorCode.message("RST-600500", e.getMessage())));
        }
        return ((ResponderRSocket) source).requestChannel(signal, payloads).subscriberContext(reactiveContext);
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return source.requestChannel(payloads);
    }

    @Override
    public Mono<Void> metadataPush(Payload payload) {
        return source.metadataPush(payload);
    }

    @Override
    public Mono<Void> onClose() {
        return source.onClose();
    }

    @Override
    public void dispose() {
        source.dispose();
    }

    public RSocket getDelegate() {
        return source;
    }
    
}
