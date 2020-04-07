package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

/**
 * Text encoding. Only for text with utf-8 encoding
 *
 * @author leijuan
 */
public class ObjectEncodingHandlerTextImpl implements ObjectEncodingHandler {
    @NotNull
    @Override
    public RSocketMimeType mimeType() {
        return RSocketMimeType.Text;
    }

    @Override
    public ByteBuf encodingParams(@Nullable Object[] args) throws EncodingException {
        if (isArrayEmpty(args)) {
            return EMPTY_BUFFER;
        }
        return Unpooled.wrappedBuffer(stringToBytes(args[0]));
    }

    @Override
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws EncodingException {
        if (data.readableBytes() > 0 && !isArrayEmpty(targetClasses)) {
            return data.toString(StandardCharsets.UTF_8);
        }
        return null;
    }

    @Override
    @NotNull
    public ByteBuf encodingResult(@Nullable Object result) throws EncodingException {
        if (result != null) {
            return Unpooled.wrappedBuffer(stringToBytes(result));
        }
        return EMPTY_BUFFER;
    }

    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws EncodingException {
        if (data.readableBytes() > 0 && targetClass != null) {
            return data.toString(StandardCharsets.UTF_8);
        }
        return null;
    }

    private byte[] stringToBytes(Object obj) throws EncodingException {
        try {
            if (obj instanceof String) {
                return ((String) obj).getBytes(StandardCharsets.UTF_8);
            } else {
                return obj.toString().getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new EncodingException(RsocketErrorCode.message("RST-700500", obj.getClass().getName(), "byte[]"), e);
        }
    }

}
