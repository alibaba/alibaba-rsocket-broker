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

    public ServiceIdRoutingMetadata() {
    }

    public ServiceIdRoutingMetadata(Integer serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.BinaryRouting;
    }

    @Override
    public String getMimeType() {
        return RSocketMimeType.BinaryRouting.getType();
    }

    @Override
    public ByteBuf getContent() {
        ByteBuf byteBuf = Unpooled.buffer(4);
        byteBuf.writeInt(this.serviceId);
        return byteBuf;
    }

    /**
     * parse data
     *
     * @param byteBuf byte buffer
     */
    public void load(ByteBuf byteBuf) {
        this.serviceId = byteBuf.readInt();
    }

    public static ServiceIdRoutingMetadata from(ByteBuf content) {
        ServiceIdRoutingMetadata temp = new ServiceIdRoutingMetadata();
        temp.load(content);
        return temp;
    }
}
