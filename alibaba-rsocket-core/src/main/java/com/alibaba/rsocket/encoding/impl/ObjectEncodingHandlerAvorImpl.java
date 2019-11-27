package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
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
    public ByteBuf encodingParams(@Nullable Object[] args) throws Exception {
        if (args != null && args.length == 1) {
            return encodingResult(args[0]);
        }
        return EMPTY_BUFFER;
    }

    @Override
    @Nullable
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws Exception {
        if (data.capacity() >= 1 && targetClasses != null && targetClasses.length == 1) {
            return decodeResult(data, targetClasses[0]);
        }
        return null;
    }

    @Override
    public ByteBuf encodingResult(@Nullable Object result) throws Exception {
        if (result instanceof SpecificRecordBase) {
            Class<?> objectClass = result.getClass();
            ByteBuffer byteBuffer = (ByteBuffer) toByteBufferMethodStore.get(objectClass).invoke(result);
            return Unpooled.wrappedBuffer(byteBuffer);
        }
        return EMPTY_BUFFER;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws Exception {
        if (data.capacity() >= 1 && targetClass != null) {
            if (SpecificRecordBase.class.equals(targetClass.getSuperclass())) {
                return fromByteBufferMethodStore.get(targetClass).invoke(null, data.nioBuffer());
            }
        }
        return null;
    }
}
