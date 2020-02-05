package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

/**
 * bearer token metadata, please refer https://github.com/rsocket/rsocket/blob/master/Extensions/Security/Authentication.md
 *
 * @author leijuan
 */
public class BearerTokenMetadata implements MetadataAware {
    /**
     * Bearer Token
     */
    private String bearerToken;

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public BearerTokenMetadata() {
    }

    public BearerTokenMetadata(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.BearerToken;
    }

    @Override
    public String getMimeType() {
        return RSocketMimeType.BearerToken.getType();
    }

    @Override
    public ByteBuf getContent() {
        byte[] credentialsBytes = bearerToken.getBytes();
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(1 + credentialsBytes.length);
        byteBuf.writeByte(0x81); //bearer type
        byteBuf.writeBytes(credentialsBytes);
        return byteBuf;
    }

    /**
     * format routing as "r:%s,s:%s" style
     *
     * @return data format
     */
    private String formatData() {
        return bearerToken;
    }

    @Override
    public String toString() {
        return formatData();
    }


    /**
     * parse data
     *
     * @param byteBuf byte buffer
     */
    public void load(ByteBuf byteBuf) throws Exception {
        //don't us regex because of performance
        String text = byteBuf.slice(1, byteBuf.capacity() - 1).toString(StandardCharsets.US_ASCII);
        load(text);
    }

    @Override
    public String toText() throws Exception {
        return formatData();
    }

    @Override
    public void load(String text) throws Exception {
        this.bearerToken = text;
    }

    public static BearerTokenMetadata jwt(String credentials) {
        return new BearerTokenMetadata(credentials);
    }

    public static BearerTokenMetadata from(ByteBuf content) {
        BearerTokenMetadata temp = new BearerTokenMetadata();
        try {
            temp.load(content);
        } catch (Exception ignore) {

        }
        return temp;
    }
}
