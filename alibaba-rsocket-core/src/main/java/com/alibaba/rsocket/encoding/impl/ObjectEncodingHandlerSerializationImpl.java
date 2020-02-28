package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Java Object Serialization encoding
 *
 * @author leijuan
 */
public class ObjectEncodingHandlerSerializationImpl implements ObjectEncodingHandler {
    @NotNull
    @Override
    public RSocketMimeType mimeType() {
        return RSocketMimeType.Java_Object;
    }

    @Override
    public ByteBuf encodingParams(@Nullable Object[] args) throws EncodingException {
        if (isArrayEmpty(args)) {
            return EMPTY_BUFFER;
        }
        return objectToByteBuf(args);
    }

    @Override
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws EncodingException {
        if (data.readableBytes() > 0 && !isArrayEmpty(targetClasses)) {
            return byteBufToObject(data);
        }
        return null;
    }

    @Override
    @NotNull
    public ByteBuf encodingResult(@Nullable Object result) throws EncodingException {
        if (result != null) {
            return objectToByteBuf(result);
        }
        return EMPTY_BUFFER;
    }

    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws EncodingException {
        if (data.readableBytes() > 0 && targetClass != null) {
            return byteBufToObject(data);
        }
        return null;
    }

    private ByteBuf objectToByteBuf(Object obj) throws EncodingException {
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            ByteBufOutputStream bos = new ByteBufOutputStream(byteBuf);
            ObjectOutputStream outputStream = new ObjectOutputStream(bos);
            outputStream.writeObject(obj);
            outputStream.close();
            return byteBuf;
        } catch (Exception e) {
            ReferenceCountUtil.safeRelease(byteBuf);
            throw new EncodingException(RsocketErrorCode.message("RST-700500", obj.getClass().getName(), "byte[]"), e);
        }
    }

    private Object byteBufToObject(ByteBuf data) throws EncodingException {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new ByteBufInputStream(data));
            Object object = inputStream.readObject();
            inputStream.close();
            return object;
        } catch (Exception e) {
            throw new EncodingException(RsocketErrorCode.message("RST-700501", "byte[]", "Object"), e);
        }
    }

}
