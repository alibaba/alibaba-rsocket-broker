package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Service ID routing metadata
 *
 * @author leijuan
 */
public class ServiceIdRoutingMetadata implements MetadataAware {
    private Integer serviceId;
    private Integer handlerId;

    public ServiceIdRoutingMetadata() {
    }

    public ServiceIdRoutingMetadata(Integer serviceId, Integer handlerId) {
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
        ByteBuf byteBuf = Unpooled.buffer(8);
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

    public static ServiceIdRoutingMetadata from(ByteBuf content) {
        ServiceIdRoutingMetadata temp = new ServiceIdRoutingMetadata();
        temp.load(content);
        return temp;
    }
}
