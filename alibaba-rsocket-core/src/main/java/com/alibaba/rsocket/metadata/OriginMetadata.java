package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * origin metadata
 *
 * @author leijuan
 */
public class OriginMetadata implements MetadataAware {
    private URI origin;

    public OriginMetadata() {

    }

    public OriginMetadata(URI origin) {
        this.origin = origin;
    }

    public URI getOrigin() {
        return origin;
    }

    public void setOrigin(URI origin) {
        this.origin = origin;
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.MessageOrigin;
    }

    @Override
    public String getMimeType() {
        return RSocketMimeType.MessageOrigin.getType();
    }

    @Override
    public ByteBuf getContent() {
        byte[] bytes = this.origin.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(bytes.length, bytes.length);
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }

    @Override
    public void load(ByteBuf byteBuf) {
        String text = byteBuf.toString(StandardCharsets.UTF_8);
        this.origin = URI.create(text);
    }

    public static OriginMetadata from(ByteBuf content) {
        OriginMetadata temp = new OriginMetadata();
        temp.load(content);
        return temp;
    }
}
