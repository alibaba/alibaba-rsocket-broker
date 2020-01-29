package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Object protobuf encoding
 *
 * @author leijuan
 */
@SuppressWarnings("unchecked")
public class ObjectEncodingHandlerProtobufImpl implements ObjectEncodingHandler {
    LoadingCache<Class<?>, Method> parseFromMethodStore = Caffeine.newBuilder()
            .maximumSize(1000)
            .build(targetClass -> targetClass.getMethod("parseFrom", ByteBuffer.class));

    @NotNull
    @Override
    public RSocketMimeType mimeType() {
        return RSocketMimeType.Protobuf;
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
        if (result != null) {
            if (result instanceof MessageLite) {
                return Unpooled.wrappedBuffer(((MessageLite) result).toByteArray());
            } else {
                LinkedBuffer buffer = LinkedBuffer.allocate(256);
                Schema schema = RuntimeSchema.getSchema(result.getClass());
                return Unpooled.wrappedBuffer(ProtostuffIOUtil.toByteArray(result, schema, buffer));
            }
        }
        return EMPTY_BUFFER;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws Exception {
        if (data.capacity() >= 1 && targetClass != null) {
            if (targetClass.getSuperclass() != null && targetClass.getSuperclass().equals(GeneratedMessageV3.class)) {
                return parseFromMethodStore.get(targetClass).invoke(null, data.nioBuffer());
            } else {
                Schema schema = RuntimeSchema.getSchema(targetClass);
                Object object = schema.newMessage();
                ProtostuffIOUtil.mergeFrom(new ByteBufInputStream(data), object, schema);
                return object;
            }
        }
        return null;
    }
}
