package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.metadata.TracingMetadataCodec;

/**
 * Tracing metadata: https://github.com/rsocket/rsocket/blob/master/Extensions/Tracing-Zipkin.md
 *
 * @author leijuan
 */
@SuppressWarnings("FieldCanBeLocal")
public class TracingMetadata implements MetadataAware {
    private long traceIdHigh;
    private long traceId;
    private boolean hasParentId;
    private long parentId;
    private long spanId;
    private boolean isEmpty;
    private boolean isNotSampled;
    private boolean isSampled;
    private boolean isDebug;

    public TracingMetadata() {
    }

    public TracingMetadata(long traceIdHigh, long traceId, long spanId, long parentId, boolean sampled, boolean debug) {
        this.traceIdHigh = traceIdHigh;
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentId = parentId;
        if (parentId != 0) {
            this.hasParentId = true;
        }
        this.isSampled = sampled;
        this.isDebug = debug;
        if (!(sampled || debug)) {
            this.isNotSampled = true;
        }
    }


    public long traceIdHigh() {
        return traceIdHigh;
    }

    /**
     * Unique 8-byte identifier for a trace, set on all spans within it.
     */
    public long traceId() {
        return traceId;
    }

    /**
     * Indicates if the parent's {@link #spanId} or if this the root span in a trace.
     */
    public final boolean hasParent() {
        return hasParentId;
    }

    /**
     * Returns the parent's {@link #spanId} where zero implies absent.
     */
    public long parentId() {
        return parentId;
    }

    /**
     * Unique 8-byte identifier of this span within a trace.
     *
     * <p>A span is uniquely identified in storage by ({@linkplain #traceId}, {@linkplain #spanId}).
     */
    public long spanId() {
        return spanId;
    }

    /**
     * Indicates that trace IDs should be accepted for tracing.
     */
    public boolean isSampled() {
        return isSampled;
    }

    /**
     * Indicates that trace IDs should be force traced.
     */
    public boolean isDebug() {
        return isDebug;
    }

    /**
     * Includes that there is sampling information and no trace IDs.
     */
    public boolean isEmpty() {
        return isEmpty;
    }

    /**
     * Indicated that sampling decision is present. If {@code false} means that decision is unknown
     * and says explicitly that {@link #isDebug()} and {@link #isSampled()} also returns {@code
     * false}.
     */
    public boolean isDecided() {
        return isNotSampled || isDebug || isSampled;
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.Tracing;
    }

    @Override
    public String getMimeType() {
        return RSocketMimeType.Tracing.getType();
    }

    @Override
    public ByteBuf getContent() {
        if (this.hasParentId) {
            return TracingMetadataCodec.encode128(PooledByteBufAllocator.DEFAULT, this.traceIdHigh, this.traceId, this.spanId, this.parentId, toFlags());
        } else {
            return TracingMetadataCodec.encode128(PooledByteBufAllocator.DEFAULT, this.traceIdHigh, this.traceId, this.spanId, toFlags());
        }
    }

    public void load(ByteBuf byteBuf) {
        io.rsocket.metadata.TracingMetadata tempTracing = TracingMetadataCodec.decode(byteBuf);
        this.traceIdHigh = tempTracing.traceIdHigh();
        this.traceId = tempTracing.traceId();
        this.spanId = tempTracing.spanId();
        this.parentId = tempTracing.parentId();
        this.hasParentId = tempTracing.hasParent();
        this.isSampled = tempTracing.isSampled();
        this.isDebug = tempTracing.isDebug();
        this.isEmpty = tempTracing.isEmpty();
    }

    public static TracingMetadata from(ByteBuf content) {
        TracingMetadata temp = new TracingMetadata();
        temp.load(content);
        return temp;
    }

    private TracingMetadataCodec.Flags toFlags() {
        if (this.isSampled) {
            return TracingMetadataCodec.Flags.SAMPLE;
        } else if (this.isDebug) {
            return TracingMetadataCodec.Flags.DEBUG;
        } else if (!this.isDecided()) {
            return TracingMetadataCodec.Flags.UNDECIDED;
        } else {
            return TracingMetadataCodec.Flags.NOT_SAMPLE;
        }
    }
}
