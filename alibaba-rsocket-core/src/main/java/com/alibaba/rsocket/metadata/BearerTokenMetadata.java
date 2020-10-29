package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.metadata.AuthMetadataCodec;
import io.rsocket.metadata.WellKnownAuthType;

/**
 * bearer token metadata, please refer https://github.com/rsocket/rsocket/blob/master/Extensions/Security/Authentication.md
 *
 * @author leijuan
 */
public class BearerTokenMetadata implements MetadataAware {
    /**
     * Bearer Token
     */
    private char[] bearerToken;

    public char[] getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(char[] bearerToken) {
        this.bearerToken = bearerToken;
    }

    public BearerTokenMetadata() {
    }

    public BearerTokenMetadata(char[] bearerToken) {
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
        return AuthMetadataCodec.encodeBearerMetadata(PooledByteBufAllocator.DEFAULT, bearerToken);
    }

    /**
     * format routing as "r:%s,s:%s" style
     *
     * @return data format
     */
    private String formatData() {
        return new String(bearerToken);
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
        WellKnownAuthType wellKnownAuthType = AuthMetadataCodec.readWellKnownAuthType(byteBuf);
        if (wellKnownAuthType == WellKnownAuthType.BEARER) {
            this.bearerToken = AuthMetadataCodec.readBearerTokenAsCharArray(byteBuf);
        }
    }

    public static BearerTokenMetadata jwt(char[] credentials) {
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
