package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

import static io.netty.buffer.Unpooled.buffer;

/**
 * Tracing metadata
 *
 * @author leijuan
 */
public class TracingMetadata implements MetadataAware {
    /**
     * trace ID bytes length
     */
    private static int ID_BYTES_LENGTH = 16;
    /**
     * tracing bytes length
     */
    private static int TRACING_BYTES_LENGTH = 33;
    /**
     * flag
     */
    private byte flag;
    /**
     * trace id: uuid  128 bit= 16 bytes
     */
    private String id;
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

    public TracingMetadata(byte flag, long id, long spanId, long parentSpanId) {
        this.flag = flag;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        ByteBuf buffer = buffer(TRACING_BYTES_LENGTH);
        buffer.writeByte(flag);
        byte[] source = id.getBytes(StandardCharsets.UTF_8);
        if (source.length == ID_BYTES_LENGTH) {
            buffer.writeBytes(source);
        } else {
            byte[] target = new byte[ID_BYTES_LENGTH];
            if (source.length > ID_BYTES_LENGTH) {
                System.arraycopy(source, 0, target, 0, ID_BYTES_LENGTH);
            } else {
                System.arraycopy(source, 0, target, ID_BYTES_LENGTH - source.length, source.length);
            }
            buffer.writeBytes(target);
        }
        buffer.writeLong(spanId);
        buffer.writeLong(parentSpanId);
        return buffer;
    }

    public void load(ByteBuf byteBuf) {
        this.flag = byteBuf.readByte();
        byte[] idBytes = new byte[ID_BYTES_LENGTH];
        byteBuf.readBytes(idBytes);
        this.id = new String(idBytes,StandardCharsets.UTF_8);
        this.spanId = byteBuf.readLong();
        this.parentSpanId = byteBuf.readLong();
    }

    @Override
    public String toText() throws Exception {
        return MessageFormat.format("%s:%s:%s", id, spanId, parentSpanId);
    }

    @Override
    public void load(String text) throws Exception {
        String[] parts = text.split(":");
        if (parts.length > 0) {
            this.id = parts[0];
        }
        if (parts.length > 1) {
            this.spanId = Long.parseLong(parts[1]);
        }
        if (parts.length > 2) {
            this.parentSpanId = Long.parseLong(parts[2]);
        }
    }

    public static TracingMetadata from(ByteBuf content) {
        TracingMetadata temp = new TracingMetadata();
        temp.load(content);
        return temp;
    }
}
