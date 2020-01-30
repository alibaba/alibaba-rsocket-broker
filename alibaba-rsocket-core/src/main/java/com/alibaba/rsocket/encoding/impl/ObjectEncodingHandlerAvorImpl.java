package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.avro.specific.SpecificRecordBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Apache Avor encoding
 *
 * @author leijuan
 */
@SuppressWarnings("unchecked")
public class ObjectEncodingHandlerAvorImpl implements ObjectEncodingHandler {
    LoadingCache<Class<?>, Method> fromByteBufferMethodStore = Caffeine.newBuilder()
            .maximumSize(1000)
            .build(targetClass -> targetClass.getMethod("fromByteBuffer", ByteBuffer.class));

    LoadingCache<Class<?>, Method> toByteBufferMethodStore = Caffeine.newBuilder()
            .maximumSize(1000)
            .build(targetClass -> targetClass.getMethod("toByteBuffer"));

    @NotNull
    @Override
    public RSocketMimeType mimeType() {
        return RSocketMimeType.Avor;
    }

    @Override
    public ByteBuf encodingParams(@Nullable Object[] args) throws EncodingException {
        if (args != null && args.length == 1) {
            return encodingResult(args[0]);
        }
        return EMPTY_BUFFER;
    }

    @Override
    @Nullable
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws EncodingException {
        if (data.capacity() >= 1 && targetClasses != null && targetClasses.length == 1) {
            return decodeResult(data, targetClasses[0]);
        }
        return null;
    }

    @Override
    public ByteBuf encodingResult(@Nullable Object result) throws EncodingException {
        if (result instanceof SpecificRecordBase) {
            Class<?> objectClass = result.getClass();
            try {
                ByteBuffer byteBuffer = (ByteBuffer) toByteBufferMethodStore.get(objectClass).invoke(result);
                return Unpooled.wrappedBuffer(byteBuffer);
            } catch (Exception e) {
                throw new EncodingException(RsocketErrorCode.message("RST-700500", result.toString(), "ByteBuf"), e);

            }
        }
        return EMPTY_BUFFER;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws EncodingException {
        if (data.capacity() >= 1 && targetClass != null) {
            if (SpecificRecordBase.class.equals(targetClass.getSuperclass())) {
                try {
                    return fromByteBufferMethodStore.get(targetClass).invoke(null, data.nioBuffer());
                } catch (Exception e) {
                    throw new EncodingException(RsocketErrorCode.message("RST-700501", "bytebuf", targetClass.getName()), e);
                }
            }
        }
        return null;
    }
}
