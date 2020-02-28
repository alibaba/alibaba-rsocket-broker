package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * binary encoding for ByteBuffer, ByteBuf and byte[]
 *
 * @author leijuan
 */
public class ObjectEncodingHandlerBinaryImpl implements ObjectEncodingHandler {
    @NotNull
    @Override
    public RSocketMimeType mimeType() {
        return RSocketMimeType.Binary;
    }

    @Override
    public ByteBuf encodingParams(@Nullable Object[] args) throws EncodingException {
        if (isArrayEmpty(args)) {
            return EMPTY_BUFFER;
        }
        return encodingResult(args[0]);
    }

    @Override
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws EncodingException {
        if (data.readableBytes() > 0 && !isArrayEmpty(targetClasses)) {
            return decodeResult(data, targetClasses[0]);
        }
        return null;
    }

    @Override
    @NotNull
    public ByteBuf encodingResult(@Nullable Object result) throws EncodingException {
        if (result != null) {
            if (result instanceof ByteBuf) {
                return (ByteBuf) result;
            } else if (result instanceof ByteBuffer) {
                return Unpooled.wrappedBuffer((ByteBuffer) result);
            } else if (result instanceof byte[]) {
                return Unpooled.wrappedBuffer((byte[]) result);
            } else {
                return Unpooled.EMPTY_BUFFER;
            }
        }

        return EMPTY_BUFFER;
    }

    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws EncodingException {
        if (data.readableBytes() > 0 && targetClass != null) {
            if (targetClass.equals(ByteBuf.class)) {
                return data;
            } else if (targetClass.equals(ByteBuffer.class)) {
                return data.nioBuffer();
            } else if (targetClass.equals(byte[].class)) {
                int length = data.readableBytes();
                byte[] content = new byte[length];
                data.readBytes(content);
                return content;
            }
        }
        return null;
    }

}
