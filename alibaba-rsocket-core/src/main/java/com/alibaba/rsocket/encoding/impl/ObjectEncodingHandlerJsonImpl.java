package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.JsonUtils;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

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
    public ByteBuf encodingParams(@Nullable Object[] args) throws EncodingException {
        if (isArrayEmpty(args)) {
            return EMPTY_BUFFER;
        }
        try {
            return JsonUtils.toJsonByteBuf(args);
        } catch (Exception e) {
            throw new EncodingException(RsocketErrorCode.message("RST-700500", "Object[]", "ByteBuf"), e);
        }
    }

    @Override
    @Nullable
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws EncodingException {
        if (data.capacity() > 0 && !isArrayEmpty(targetClasses)) {
            try {
                return JsonUtils.readJsonArray(data, targetClasses);
            } catch (Exception e) {
                throw new EncodingException(RsocketErrorCode.message("RST-700501", "bytebuf", Arrays.toString(targetClasses)), e);
            }
        }
        return null;
    }

    @Override
    @NotNull
    public ByteBuf encodingResult(@Nullable Object result) throws EncodingException {
        if (result == null) {
            return EMPTY_BUFFER;
        }
        try {
            return JsonUtils.toJsonByteBuf(result);
        } catch (Exception e) {
            throw new EncodingException(RsocketErrorCode.message("RST-700500", result.getClass().getCanonicalName(), "ByteBuf"), e);
        }
    }

    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws EncodingException {
        if (data.capacity() > 0 && targetClass != null) {
            try {
                return JsonUtils.readJsonValue(data, targetClass);
            } catch (Exception e) {
                throw new EncodingException(RsocketErrorCode.message("RST-700501", "bytebuf", targetClass.getName()), e);
            }
        }
        return null;
    }
}
