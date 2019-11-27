package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

/**
 * bearer token metadata, please refer https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication
 *
 * @author leijuan
 */
public class BearerTokenMetadata implements MetadataAware {
    /**
     * credentials,
     */
    private String credentials;

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public BearerTokenMetadata() {
    }

    public BearerTokenMetadata(String credentials) {
        this.credentials = credentials;
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
        return Unpooled.wrappedBuffer(credentials.getBytes());
    }

    /**
     * format routing as "r:%s,s:%s" style
     *
     * @return data format
     */
    private String formatData() {
        return credentials;
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
        String text = byteBuf.toString(StandardCharsets.US_ASCII);
        load(text);
    }

    @Override
    public String toText() throws Exception {
        return formatData();
    }

    @Override
    public void load(String text) throws Exception {
        this.credentials = text;
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
