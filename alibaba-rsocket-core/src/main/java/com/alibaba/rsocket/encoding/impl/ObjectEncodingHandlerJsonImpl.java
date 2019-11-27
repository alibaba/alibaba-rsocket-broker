package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.JsonUtils;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Object json encoding
 *
 * @author leijuan
 */
public class ObjectEncodingHandlerJsonImpl implements ObjectEncodingHandler {
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
        return Unpooled.wrappedBuffer(JsonUtils.toJsonBytes(args));
    }

    @Override
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws Exception {
        if (data.capacity() >= 1 && targetClasses != null && targetClasses.length > 0) {
            return JsonUtils.readJsonArray(data, targetClasses);
        }
        return null;
    }

    @Override
    public ByteBuf encodingResult(@Nullable Object result) throws Exception {
        if (result != null) {
            return Unpooled.wrappedBuffer(JsonUtils.toJsonBytes(result));
        }
        return EMPTY_BUFFER;
    }

    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws Exception {
        if (data.capacity() >= 1 && targetClass != null) {
            return JsonUtils.readJsonValue(data, targetClass);
        }
        return null;
    }
}
