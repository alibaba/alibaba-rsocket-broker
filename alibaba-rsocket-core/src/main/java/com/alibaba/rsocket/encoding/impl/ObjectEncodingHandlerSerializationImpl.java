package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
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
        if (args == null || args.length == 0 || args[0] == null) {
            return EMPTY_BUFFER;
        }
        return Unpooled.wrappedBuffer(objectToBytes(args));
    }

    @Override
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws EncodingException {
        if (data.capacity() >= 1 && targetClasses != null && targetClasses.length > 0) {
            return objectToBytes(data);
        }
        return null;
    }

    @Override
    @NotNull
    public ByteBuf encodingResult(@Nullable Object result) throws EncodingException {
        if (result != null) {
            return Unpooled.wrappedBuffer(objectToBytes(result));
        }
        return EMPTY_BUFFER;
    }

    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws EncodingException {
        if (data.capacity() >= 1 && targetClass != null) {
            return bytesToObject(data);
        }
        return null;
    }

    private byte[] objectToBytes(Object obj) throws EncodingException {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(bos);
            outputStream.writeObject(obj);
            outputStream.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new EncodingException(RsocketErrorCode.message("RST-700500", obj.getClass().getName(), "byte[]"), e);
        }
    }

    private Object bytesToObject(ByteBuf data) throws EncodingException {
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
