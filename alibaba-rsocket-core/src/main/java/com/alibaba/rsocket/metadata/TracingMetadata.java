package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * Tracing metadata: https://github.com/rsocket/rsocket/blob/master/Extensions/Tracing-Zipkin.md
 *
 * @author leijuan
 */
@SuppressWarnings("FieldCanBeLocal")
public class TracingMetadata implements MetadataAware {
    private static int TRACING_BYTES_MAX_LENGTH = 33;
    private static byte TRACE_ID_128_MARK = (byte) 0x80;
    private static byte PARENT_SPAN_MARK = (byte) 0x40;
    /**
     * flag
     */
    private byte flag = 0;
    /**
     * Upper 64bits of the trace ID
     */
    private long traceIdHigh;
    /**
     * Lower 64bits of the trace ID
     */
    private long traceIdLow;
    /**
     * span id
     */
    private long spanId;
    /**
     * parent span id
     */
    private long parentSpanId;

    public TracingMetadata() {
    }

    public TracingMetadata(long traceIdHigh, long traceIdLow, long spanId, long parentSpanId) {
        this.setTraceIdHigh(traceIdHigh);
        this.traceIdLow = traceIdLow;
        this.spanId = spanId;
        this.setParentSpanId(parentSpanId);
    }

    public void sampling(boolean reported) {
        if (reported) {
            flag = (byte) (flag | 0x30);
        }
    }

    public boolean isSamplingReported() {
        return (flag & 0x30) == 0;
    }

    public void debug(boolean reported) {
        if (reported) {
            flag = (byte) (flag | 0xC0);
        }
    }

    public boolean isDebugReported() {
        return (flag & 0xC0) == 0;
    }

    public long getTraceIdHigh() {
        return traceIdHigh;
    }

    public void setTraceIdHigh(long traceIdHigh) {
        this.traceIdHigh = traceIdHigh;
        if (traceIdHigh > 0) {
            flag = (byte) (flag | TRACE_ID_128_MARK);
        }
    }

    public long getTraceIdLow() {
        return traceIdLow;
    }

    public void setTraceIdLow(long traceIdLow) {
        this.traceIdLow = traceIdLow;
    }

    public void setSpanId(long spanId) {
        this.spanId = spanId;
    }

    public void setParentSpanId(long parentSpanId) {
        this.parentSpanId = parentSpanId;
        if (traceIdHigh > 0) {
            flag = (byte) (flag | TRACE_ID_128_MARK);
        }
    }

    public Long getSpanId() {
        return spanId;
    }

    public void setSpanId(Long spanId) {
        this.spanId = spanId;
    }

    public Long getParentSpanId() {
        return parentSpanId;
    }

    public void setParentSpanId(Long parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public byte getFlag() {
        return flag;
    }

    public void setFlag(byte flag) {
        this.flag = flag;
    }

    private int byteBufCapacity() {
        int len = TRACING_BYTES_MAX_LENGTH;
        if (this.traceIdHigh == 0) {
            len = len - 8;
        }
        if (this.parentSpanId == 0) {
            len = len - 8;
        }
        return len;
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
        int bytesLength = byteBufCapacity();
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(bytesLength, bytesLength);
        buffer.writeByte(flag);
        if (this.traceIdHigh > 0) {
            buffer.writeLong(this.traceIdHigh);
        }
        buffer.writeLong(this.traceIdLow);
        buffer.writeLong(spanId);
        if (this.parentSpanId > 0) {
            buffer.writeLong(parentSpanId);
        }
        return buffer;
    }

    public void load(ByteBuf byteBuf) {
        this.flag = byteBuf.readByte();
        if ((flag & TRACE_ID_128_MARK) > 0) {
            this.traceIdHigh = byteBuf.readLong();
        }
        this.traceIdLow = byteBuf.readLong();
        this.spanId = byteBuf.readLong();
        if ((flag & PARENT_SPAN_MARK) > 0) {
            this.parentSpanId = byteBuf.readLong();
        }
    }

    public static TracingMetadata from(ByteBuf content) {
        TracingMetadata temp = new TracingMetadata();
        temp.load(content);
        return temp;
    }
}
