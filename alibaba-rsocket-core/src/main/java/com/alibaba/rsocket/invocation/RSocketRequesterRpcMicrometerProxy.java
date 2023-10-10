package com.alibaba.rsocket.invocation;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.metadata.TracingMetadata;
import com.alibaba.rsocket.upstream.UpstreamManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.Payload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;


/**
 * RSocket requester RPC proxy for remote service with micrometer tracing support
 *
 * @author amondnet
 */
public class RSocketRequesterRpcMicrometerProxy extends RSocketRequesterRpcProxy {
    private Tracer tracer;

    public RSocketRequesterRpcMicrometerProxy(@NotNull Tracer tracer, UpstreamManager upstreamManager,
                                              String group, Class<?> serviceInterface, @Nullable String service, String version,
                                              RSocketMimeType encodingType, @Nullable RSocketMimeType acceptEncodingType,
                                              Duration timeout, @Nullable String endpoint, boolean sticky, URI sourceUri, boolean jdkProxy) {
        super(upstreamManager, group, serviceInterface, service, version, encodingType, acceptEncodingType, timeout, endpoint, sticky, sourceUri, jdkProxy);
        this.tracer = tracer;
    }

    @NotNull
    protected Mono<Payload> remoteRequestResponse(ReactiveMethodMetadata methodMetadata, ByteBuf compositeMetadata, ByteBuf bodyBuf) {
        return Mono.deferContextual(context -> {
            TraceContext traceContext = context.getOrDefault(TraceContext.class, null);
            if (traceContext != null) {
                CompositeByteBuf newCompositeMetadata = new CompositeByteBuf(PooledByteBufAllocator.DEFAULT, true, 2, compositeMetadata, tracingMetadata(traceContext).getContent());
                Span span = tracer.nextSpan();
                return super.remoteRequestResponse(methodMetadata, newCompositeMetadata, bodyBuf)
                        .doOnError(span::error)
                        .doOnSuccess(payload -> span.end());
            }
            return super.remoteRequestResponse(methodMetadata, compositeMetadata, bodyBuf);
        });
    }

    @Override
    protected Mono<Void> remoteFireAndForget(ReactiveMethodMetadata methodMetadata, ByteBuf compositeMetadata, ByteBuf bodyBuf) {
        return Mono.deferContextual(context -> {
            TraceContext traceContext = context.getOrDefault(TraceContext.class, null);
            if (traceContext != null) {
                CompositeByteBuf newCompositeMetadata = new CompositeByteBuf(PooledByteBufAllocator.DEFAULT, true, 2, compositeMetadata, tracingMetadata(traceContext).getContent());
                Span span = tracer.nextSpan();
                return super.remoteFireAndForget(methodMetadata, newCompositeMetadata, bodyBuf)
                        .doOnError(span::error)
                        .doOnSuccess(payload -> span.end());
            }
            return super.remoteFireAndForget(methodMetadata, compositeMetadata, bodyBuf);
        });
    }

    @Override
    protected Flux<Payload> remoteRequestStream(ReactiveMethodMetadata methodMetadata, ByteBuf compositeMetadata, ByteBuf bodyBuf) {
        return Flux.deferContextual(context -> {
            TraceContext traceContext = context.getOrDefault(TraceContext.class, null);
            if (traceContext != null) {
                CompositeByteBuf newCompositeMetadata = new CompositeByteBuf(PooledByteBufAllocator.DEFAULT, true, 2, compositeMetadata, tracingMetadata(traceContext).getContent());
                Span span = tracer.nextSpan();
                return super.remoteRequestStream(methodMetadata, newCompositeMetadata, bodyBuf)
                        .doOnError(span::error)
                        .doOnComplete(span::end);
            }
            return super.remoteRequestStream(methodMetadata, compositeMetadata, bodyBuf);
        });
    }

    public TracingMetadata tracingMetadata(TraceContext traceContext) {
        // 1 ~ 32 character
        String traceIdValue = traceContext.traceId();
        String traceIdHighHexString = traceIdValue.substring(0, 16);
        String traceIdHexString = traceIdValue.substring(traceIdValue.length() - 16);
        long traceIdHigh = Long.parseUnsignedLong(traceIdHighHexString, 16);
        long traceId = Long.parseUnsignedLong(traceIdHexString, 16);
        long spanId = Long.parseUnsignedLong(traceContext.spanId(), 16);
        long parentId = 0;
        if ( traceContext.parentId() != null ) {
            parentId = Long.parseUnsignedLong(Objects.requireNonNull(traceContext.parentId()), 16);
        }
        return new TracingMetadata(traceIdHigh, traceId, spanId, parentId, true, false);
    }
}
