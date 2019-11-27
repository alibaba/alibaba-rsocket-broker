package com.alibaba.rsocket.encoding;

import com.alibaba.rsocket.encoding.impl.RSocketEncodingFacadeImpl;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import io.netty.buffer.ByteBuf;
import io.rsocket.Payload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.Null;

/**
 * RSocket Encoding Facade
 *
 * @author leijuan
 */
public interface RSocketEncodingFacade {

    @NotNull
    ByteBuf encodingParams(@Nullable Object[] args, RSocketMimeType encodingType);

    @Nullable
    Object decodeParams(RSocketMimeType encodingType, @Nullable ByteBuf data, @Nullable Class<?>... targetClasses);

    @NotNull
    ByteBuf encodingResult(@Nullable Object result, RSocketMimeType encodingType);

    @Nullable
    Object decodeResult(RSocketMimeType encodingType, @Nullable ByteBuf data, @Nullable Class<?> targetClass);

    /**
     * get RSocket encoding facade singleton
     *
     * @return encoding facade
     */
    static RSocketEncodingFacade getInstance() {
        return RSocketEncodingFacadeImpl.Instance;
    }
}
