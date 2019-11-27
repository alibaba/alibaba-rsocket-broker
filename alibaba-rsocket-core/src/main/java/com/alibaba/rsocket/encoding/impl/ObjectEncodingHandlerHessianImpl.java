package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.HessianUtils;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public ByteBuf encodingParams(@Nullable Object[] args) throws Exception {
        if (args == null || args.length == 0 || args[0] == null) {
            return EMPTY_BUFFER;
        }
        return HessianUtils.outputAsBuffer(args);
    }

    @Override
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws Exception {
        if (data.capacity() >= 1) {
            return HessianUtils.decode(data);
        }
        return null;
    }

    @Override
    public ByteBuf encodingResult(@Nullable Object result) throws Exception {
        if (result == null) {
            return EMPTY_BUFFER;
        }
        return HessianUtils.outputAsBuffer(result);
    }

    @Override
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws Exception {
        if (data.capacity() >= 1) {
            return HessianUtils.decode(data);
        }
        return null;
    }
}
