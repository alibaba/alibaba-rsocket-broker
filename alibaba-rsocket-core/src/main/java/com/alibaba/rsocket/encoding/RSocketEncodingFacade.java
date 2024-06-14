package com.alibaba.rsocket.encoding;

import com.alibaba.rsocket.encoding.impl.RSocketEncodingFacadeImpl;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * RSocket Encoding Facade
 *
 * @author leijuan
 */
public interface RSocketEncodingFacade {

    @NotNull
    ByteBuf encodingParams(@Nullable Object[] args, RSocketMimeType encodingType) throws EncodingException;

    @Nullable
    Object decodeParams(RSocketMimeType encodingType, @Nullable ByteBuf data, @Nullable Class<?>... targetClasses) throws EncodingException;

    @NotNull
    ByteBuf encodingResult(@Nullable Object result, RSocketMimeType encodingType) throws EncodingException;

    @Nullable
    Object decodeResult(RSocketMimeType encodingType, @Nullable ByteBuf data, @Nullable Class<?> targetClass) throws EncodingException;

    /**
     * get RSocket encoding facade singleton
     *
     * @return encoding facade
     */
//    static RSocketEncodingFacade getInstance() {
//        return RSocketEncodingFacadeImpl.instance;
//    }
    static RSocketEncodingFacade getInstance(RSocketEncodingFacadeImpl impl) {
        return impl;
    }
}
