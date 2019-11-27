package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
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
        return RSocketMimeType.Json;
    }

    @Override
    public ByteBuf encodingParams(@Nullable Object[] args) throws Exception {
        if (args == null || args.length == 0 || args[0] == null) {
            return EMPTY_BUFFER;
        }
        return Unpooled.wrappedBuffer(objectToBytes(args));
    }

    @Override
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws Exception {
        if (data.capacity() >= 1 && targetClasses != null && targetClasses.length > 0) {
            return objectToBytes(data);
        }
        return null;
    }

    @Override
    public ByteBuf encodingResult(@Nullable Object result) throws Exception {
        if (result != null) {
            return Unpooled.wrappedBuffer(objectToBytes(result));
        }
        return EMPTY_BUFFER;
    }

    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws Exception {
        if (data.capacity() >= 1 && targetClass != null) {
            return bytesToObject(data);
        }
        return null;
    }

    private byte[] objectToBytes(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(bos);
        outputStream.writeObject(obj);
        outputStream.flush();
        return bos.toByteArray();
    }

    private Object bytesToObject(ByteBuf data) throws Exception {
        ObjectInputStream inputStream = new ObjectInputStream(new ByteBufInputStream(data));
        Object object = inputStream.readObject();
        inputStream.close();
        return object;
    }
}
