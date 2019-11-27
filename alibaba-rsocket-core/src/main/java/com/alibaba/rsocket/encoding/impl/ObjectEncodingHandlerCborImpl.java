package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.JsonUtils;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * Object cbor encoding
 *
 * @author leijuan
 */
public class ObjectEncodingHandlerCborImpl implements ObjectEncodingHandler {
    private ObjectMapper objectMapper = new ObjectMapper(new CBORFactory());

    @NotNull
    @Override
    public RSocketMimeType mimeType() {
        return RSocketMimeType.CBOR;
    }

    @Override
    public ByteBuf encodingParams(@Nullable Object[] args) throws Exception {
        if (args == null || args.length == 0 || args[0] == null) {
            return EMPTY_BUFFER;
        }
        return Unpooled.wrappedBuffer(objectMapper.writeValueAsBytes(args[0]));
    }

    @Override
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws Exception {
        if (data.capacity() >= 1 && targetClasses != null && targetClasses.length > 0) {
            return objectMapper.readValue((InputStream) new ByteBufInputStream(data), targetClasses[0]);
        }
        return null;
    }

    @Override
    public ByteBuf encodingResult(@Nullable Object result) throws Exception {
        if (result != null) {
            return Unpooled.wrappedBuffer(objectMapper.writeValueAsBytes(result));
        }
        return EMPTY_BUFFER;
    }

    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws Exception {
        if (data.capacity() >= 1 && targetClass != null) {
            return objectMapper.readValue((InputStream) new ByteBufInputStream(data), targetClass);
        }
        return null;
    }
}
