package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * cache control metadata
 *
 * @author leijuan
 */
public class CacheControlMetadata implements MetadataAware {
    private static int BYTES_LENGTH = 8;
    /**
     * expired timestamp
     */
    private Long expiredAt;

    public CacheControlMetadata() {
    }

    public CacheControlMetadata(Long expiredAt) {
        this.expiredAt = expiredAt;
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.CacheControl;
    }

    @Override
    public String getMimeType() {
        return RSocketMimeType.CacheControl.getType();
    }

    @Override
    public ByteBuf getContent() {
        return Unpooled.copyLong(expiredAt);
    }

    @Override
    public String toString() {
        return String.valueOf(expiredAt);
    }

    /**
     * parse data
     *
     * @param byteBuf byte buffer
     */
    public void load(ByteBuf byteBuf) {
        this.expiredAt = byteBuf.readLong();
    }

    @Override
    public String toText() throws Exception {
        return toString();
    }

    @Override
    public void load(String text) throws Exception {
        this.expiredAt = Long.valueOf(text);
    }

    public static CacheControlMetadata from(ByteBuf content) {
        CacheControlMetadata temp = new CacheControlMetadata();
        temp.load(content);
        return temp;
    }
}
