package com.alibaba.rsocket.broker.api.converter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDataBufferDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Map;

/**
 * ByteBuf Decoder
 *
 * @author leijuan
 */
public class ByteBufDecoder extends AbstractDataBufferDecoder<ByteBuf> {
    public ByteBufDecoder() {
        super(MimeTypeUtils.ALL);
    }

    @Override
    public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
        return (ByteBuf.class.isAssignableFrom(elementType.toClass()) &&
                super.canDecode(elementType, mimeType));
    }

    @Override
    public ByteBuf decode(DataBuffer dataBuffer, ResolvableType elementType,
                          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        if (dataBuffer instanceof NettyDataBuffer) {
            return ((NettyDataBuffer) dataBuffer).getNativeBuffer();
        }
        return Unpooled.wrappedBuffer(dataBuffer.asByteBuffer());
    }
}
