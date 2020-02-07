package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.HessianUtils;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * object encoding handler hessian implementation
 *
 * @author leijuan
 */
public class ObjectEncodingHandlerHessianImpl implements ObjectEncodingHandler {
    @NotNull
    @Override
    public RSocketMimeType mimeType() {
        return RSocketMimeType.Hessian;
    }

    @Override
    public ByteBuf encodingParams(@Nullable Object[] args) throws EncodingException {
        if (isArrayEmpty(args)) {
            return EMPTY_BUFFER;
        }
        try {
            return HessianUtils.encode(args);
        } catch (Exception e) {
            throw new EncodingException(RsocketErrorCode.message("RST-700500", "object[]", "ByteBuf"), e);
        }
    }

    @Override
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws EncodingException {
        if (data.capacity() > 0) {
            try {
                return HessianUtils.decode(data);
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
            return HessianUtils.encode(result);
        } catch (Exception e) {
            throw new EncodingException(RsocketErrorCode.message("RST-700500", result.toString(), "ByteBuf"), e);
        }
    }

    @Override
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws EncodingException {
        if (data.capacity() > 0) {
            try {
                return HessianUtils.decode(data);
            } catch (Exception e) {
                throw new EncodingException(RsocketErrorCode.message("RST-700501", "bytebuf", targetClass == null ? "" : targetClass.getName()), e);
            }
        }
        return null;
    }
}
