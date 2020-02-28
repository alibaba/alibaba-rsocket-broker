package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
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
        return encode(args);
    }

    @Override
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws EncodingException {
        if (data.readableBytes() > 0) {
            try {
                return decode(data);
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
        return encode(result);
    }

    @Override
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws EncodingException {
        if (data.readableBytes() > 0) {
            try {
                return decode(data);
            } catch (Exception e) {
                throw new EncodingException(RsocketErrorCode.message("RST-700501", "bytebuf", targetClass == null ? "" : targetClass.getName()), e);
            }
        }
        return null;
    }

    public static ByteBuf encode(Object obj) throws EncodingException {
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            ByteBufOutputStream bos = new ByteBufOutputStream(byteBuf);
            HessianSerializerOutput output = new HessianSerializerOutput(bos);
            output.writeObject(obj);
            output.flush();
            return byteBuf;
        } catch (Exception e) {
            ReferenceCountUtil.safeRelease(byteBuf);
            throw new EncodingException(RsocketErrorCode.message("RST-700500", obj.getClass().getName(), "byte[]"), e);
        }
    }

    public static Object decode(@Nullable ByteBuf byteBuf) throws IOException {
        if (byteBuf == null || byteBuf.readableBytes() == 0) {
            return null;
        }
        return new HessianSerializerInput(new ByteBufInputStream(byteBuf)).readObject();
    }
}
