package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * CloudEvents encoding
 *
 * @author leijuan
 */
public class ObjectEncodingHandlerCloudEventsImpl implements ObjectEncodingHandler {
    private static EventFormat EVENT_FORMAT = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);

    @NotNull
    @Override
    public RSocketMimeType mimeType() {
        return RSocketMimeType.CloudEventsJson;
    }

    @Override
    public ByteBuf encodingParams(@Nullable Object[] args) throws EncodingException {
        if (args != null && args.length == 1) {
            return encodingResult(args[0]);
        }
        return EMPTY_BUFFER;
    }

    @Override
    @Nullable
    public Object decodeParams(ByteBuf data, @Nullable Class<?>... targetClasses) throws EncodingException {
        if (data.readableBytes() > 0 && targetClasses != null && Objects.equals(targetClasses[0], CloudEvent.class)) {
            return decodeResult(data, targetClasses[0]);
        }
        return null;
    }

    @Override
    @NotNull
    public ByteBuf encodingResult(@Nullable Object result) throws EncodingException {
        if (result instanceof CloudEvent) {
            return Unpooled.wrappedBuffer(EVENT_FORMAT.serialize((CloudEvent) result));
        }
        return EMPTY_BUFFER;
    }

    @Override
    @Nullable
    public Object decodeResult(ByteBuf data, @Nullable Class<?> targetClass) throws EncodingException {
        if (data.readableBytes() > 0 && Objects.equals(targetClass, CloudEvent.class)) {
            int length = data.readableBytes();
            byte[] content = new byte[length];
            data.readBytes(content);
            return EVENT_FORMAT.deserialize(content);
        }
        return null;
    }
}
