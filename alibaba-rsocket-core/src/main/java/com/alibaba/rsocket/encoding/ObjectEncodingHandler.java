package com.alibaba.rsocket.encoding;

import com.alibaba.rsocket.metadata.RSocketMimeType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * RSocket Mime type encoding Handler
 *
 * @author leijuan
 */
public interface ObjectEncodingHandler {
    ByteBuf EMPTY_BUFFER = Unpooled.EMPTY_BUFFER;

    /**
     * rsocket mime type
     *
     * @return rsocket mime type
     */
    @NotNull
    RSocketMimeType mimeType();

    /**
     * encoding params
     *
     * @param args arguments
     * @return byte buffer
     * @throws EncodingException encoding exception
     */
    ByteBuf encodingParams(@Nullable Object[] args) throws EncodingException;

    /**
     * decode params, the return value maybe array or single object value
     *
     * @param data          data
     * @param targetClasses target classes
     * @return object array or single object value
     * @throws EncodingException exception
     */
    @Nullable
    Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws EncodingException;

    /**
     * encode result
     *
     * @param result result
     * @return byte buffer
     * @throws EncodingException exception
     */
    @NotNull
    ByteBuf encodingResult(@Nullable Object result) throws EncodingException;

    /**
     * decode result
     *
     * @param data        data
     * @param targetClass target class
     * @return result object
     * @throws EncodingException exception
     */
    @Nullable
    Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws EncodingException;


    default boolean isArrayEmpty(Object[] args) {
        return args == null || args.length == 0 || (args.length == 1 && args[0] == null);
    }
}
