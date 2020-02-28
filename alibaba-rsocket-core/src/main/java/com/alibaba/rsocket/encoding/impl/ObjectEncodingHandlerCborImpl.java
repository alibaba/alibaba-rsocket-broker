package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

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
    public ByteBuf encodingParams(@Nullable Object[] args) throws EncodingException {
        if (isArrayEmpty(args)) {
            return EMPTY_BUFFER;
        }
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            ByteBufOutputStream bos = new ByteBufOutputStream(byteBuf);
            //noinspection ConstantConditions
            objectMapper.writeValue((OutputStream) bos, args[0]);
            return byteBuf;
        } catch (Exception e) {
            ReferenceCountUtil.safeRelease(byteBuf);
            throw new EncodingException(RsocketErrorCode.message("RST-700500", Arrays.toString(args), "Bytebuf"), e);
        }
    }

    @Override
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws EncodingException {
        if (data.readableBytes() > 0 && targetClasses != null && targetClasses.length > 0) {
            try {
                return objectMapper.readValue((InputStream) new ByteBufInputStream(data), targetClasses[0]);
            } catch (Exception e) {
                throw new EncodingException(RsocketErrorCode.message("RST-700501", "ByteBuf", Arrays.toString(targetClasses)), e);
            }
        }
        return null;
    }

    @Override
    @NotNull
    public ByteBuf encodingResult(@Nullable Object result) throws EncodingException {
        if (result != null) {
            ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer();
            try {
                ByteBufOutputStream bos = new ByteBufOutputStream(byteBuf);
                objectMapper.writeValue((OutputStream) bos, result);
                return byteBuf;
            } catch (Exception e) {
                ReferenceCountUtil.safeRelease(byteBuf);
                throw new EncodingException(RsocketErrorCode.message("RST-700500", result.toString(), "Bytebuf"), e);
            }
        }
        return EMPTY_BUFFER;
    }

    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws EncodingException {
        if (data.readableBytes() > 0 && targetClass != null) {
            try {
                return objectMapper.readValue((InputStream) new ByteBufInputStream(data), targetClass);
            } catch (Exception e) {
                throw new EncodingException(RsocketErrorCode.message("RST-700501", "ByteBuf", targetClass.getName()), e);
            }
        }
        return null;
    }
}
