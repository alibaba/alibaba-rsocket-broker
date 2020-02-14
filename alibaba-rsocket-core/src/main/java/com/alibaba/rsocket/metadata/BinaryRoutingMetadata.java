package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.util.NumberUtils;

/**
 * binary routing metadata with service id and handler id
 *
 * @author leijuan
 */
public class BinaryRoutingMetadata implements MetadataAware {
    private Integer serviceId;
    private Integer handlerId;
    private byte[] metadata;

    public BinaryRoutingMetadata() {
    }

    public BinaryRoutingMetadata(Integer serviceId, Integer handlerId) {
        this.serviceId = serviceId;
        this.handlerId = handlerId;
    }

    public BinaryRoutingMetadata(Integer serviceId, Integer handlerId, byte[] metadata) {
        this.serviceId = serviceId;
        this.handlerId = handlerId;
        this.metadata = metadata;
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.BinaryRouting;
    }

    @Override
    public String getMimeType() {
        return RSocketMimeType.BinaryRouting.getType();
    }

    public Integer getServiceId() {
        return serviceId;
    }

    public Integer getHandlerId() {
        return handlerId;
    }

    public byte[] getMetadata() {
        return metadata;
    }

    public void setMetadata(byte[] metadata) {
        this.metadata = metadata;
    }

    @Override
    public ByteBuf getContent() {
        int capacity = 8;
        if (this.metadata != null) {
            capacity = 8 + this.metadata.length;
        }
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(capacity, capacity);
        byteBuf.writeInt(this.serviceId);
        byteBuf.writeInt(this.handlerId);
        if (this.metadata != null) {
            byteBuf.writeBytes(this.metadata);
        }
        return byteBuf;
    }


    public ByteBuf getHeaderAndContent() {
        int capacity = 12;
        if (this.metadata != null) {
            capacity = 12 + this.metadata.length;
        }
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(capacity, capacity);
        byteBuf.writeByte(WellKnownMimeType.MESSAGE_RSOCKET_BINARY_ROUTING.getIdentifier() | 0x80);
        NumberUtils.encodeUnsignedMedium(byteBuf, capacity - 4);
        byteBuf.writeInt(this.serviceId);
        byteBuf.writeInt(this.handlerId);
        if (this.metadata != null) {
            byteBuf.writeBytes(this.metadata);
        }
        return byteBuf;
    }

    /**
     * parse data
     *
     * @param byteBuf byte buffer
     */
    public void load(ByteBuf byteBuf) {
        this.serviceId = byteBuf.readInt();
        this.handlerId = byteBuf.readInt();
        int readableBytesLen = byteBuf.readableBytes();
        if (readableBytesLen > 0) {
            this.metadata = new byte[readableBytesLen];
            byteBuf.readBytes(this.metadata);
        }
    }

    public static BinaryRoutingMetadata from(ByteBuf content) {
        BinaryRoutingMetadata temp = new BinaryRoutingMetadata();
        temp.load(content);
        return temp;
    }
}
