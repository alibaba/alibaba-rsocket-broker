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
    private int flags = 0;
    private byte[] routingText;

    public BinaryRoutingMetadata() {
    }

    public BinaryRoutingMetadata(Integer serviceId, Integer handlerId) {
        this.serviceId = serviceId;
        this.handlerId = handlerId;
        this.flags = 0;
    }

    public BinaryRoutingMetadata(Integer serviceId, Integer handlerId, byte[] routingText) {
        this.serviceId = serviceId;
        this.handlerId = handlerId;
        this.routingText = routingText;
        this.flags = 0;
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

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public boolean isSticky() {
        return (this.flags & 0x01) == 0x01;
    }

    public void setSticky(boolean sticky) {
        if (sticky) {
            this.flags = this.flags | 0x01;
        } else {
            this.flags = this.flags & (Integer.MAX_VALUE - 1);
        }
    }

    public byte[] getRoutingText() {
        return routingText;
    }

    public void setRoutingText(byte[] routingText) {
        this.routingText = routingText;
    }

    @Override
    public ByteBuf getContent() {
        int capacity = 12;
        if (this.routingText != null) {
            capacity = 12 + this.routingText.length;
        }
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(capacity, capacity);
        byteBuf.writeInt(this.serviceId);
        byteBuf.writeInt(this.handlerId);
        byteBuf.writeInt(this.flags);
        if (this.routingText != null) {
            byteBuf.writeBytes(this.routingText);
        }
        return byteBuf;
    }


    public ByteBuf getHeaderAndContent() {
        int capacity = 16;
        if (this.routingText != null) {
            capacity = 16 + this.routingText.length;
        }
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(capacity, capacity);
        byteBuf.writeByte(WellKnownMimeType.MESSAGE_RSOCKET_BINARY_ROUTING.getIdentifier() | 0x80);
        NumberUtils.encodeUnsignedMedium(byteBuf, capacity - 4);
        byteBuf.writeInt(this.serviceId);
        byteBuf.writeInt(this.handlerId);
        byteBuf.writeInt(this.flags);
        if (this.routingText != null) {
            byteBuf.writeBytes(this.routingText);
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
        this.flags = byteBuf.readInt();
        int readableBytesLen = byteBuf.readableBytes();
        if (readableBytesLen > 0) {
            this.routingText = new byte[readableBytesLen];
            byteBuf.readBytes(this.routingText);
        }
    }

    public static BinaryRoutingMetadata from(ByteBuf content) {
        BinaryRoutingMetadata temp = new BinaryRoutingMetadata();
        temp.load(content);
        return temp;
    }
}
