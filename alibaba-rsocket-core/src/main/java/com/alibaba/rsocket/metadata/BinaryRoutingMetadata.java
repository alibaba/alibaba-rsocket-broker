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

    public BinaryRoutingMetadata() {
    }

    public BinaryRoutingMetadata(Integer serviceId, Integer handlerId) {
        this.serviceId = serviceId;
        this.handlerId = handlerId;
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

    @Override
    public ByteBuf getContent() {
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(8, 8);
        byteBuf.writeInt(this.serviceId);
        byteBuf.writeInt(this.handlerId);
        return byteBuf;
    }


    public ByteBuf getHeaderAndContent() {
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(12, 12);
        byteBuf.writeByte(WellKnownMimeType.MESSAGE_RSOCKET_BINARY_ROUTING.getIdentifier() | 0x80);
        NumberUtils.encodeUnsignedMedium(byteBuf, 8);
        byteBuf.writeInt(this.serviceId);
        byteBuf.writeInt(this.handlerId);
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
    }

    public static BinaryRoutingMetadata from(ByteBuf content) {
        BinaryRoutingMetadata temp = new BinaryRoutingMetadata();
        temp.load(content);
        return temp;
    }
}
