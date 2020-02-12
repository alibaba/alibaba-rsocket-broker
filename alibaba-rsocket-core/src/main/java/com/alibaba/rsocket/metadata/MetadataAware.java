package com.alibaba.rsocket.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.netty.buffer.ByteBuf;
import io.rsocket.metadata.CompositeMetadata;
import reactor.util.annotation.Nullable;

/**
 * metadata aware
 *
 * @author leijuan
 */
public interface MetadataAware extends CompositeMetadata.Entry {
    /**
     * metadata mime name
     *
     * @return RSocket MIME type
     */
    RSocketMimeType rsocketMimeType();

    @Nullable
    @JsonIgnore
    String getMimeType();

    @JsonIgnore
    ByteBuf getContent();

    /**
     * load metadata from byte buffer
     *
     * @param byteBuf byte buf
     * @throws Exception exception
     */
    void load(ByteBuf byteBuf) throws Exception;

}
