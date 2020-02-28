package com.alibaba.rsocket.broker.api.converter;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * ByteBuf encoder
 *
 * @author leijuan
 */
public class ByteBufEncoder extends AbstractEncoder<ByteBuf> {
    public ByteBufEncoder() {
        super(MimeTypeUtils.ALL);
    }


    @Override
    public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
        Class<?> clazz = elementType.toClass();
        return super.canEncode(elementType, mimeType) && ByteBuf.class.isAssignableFrom(clazz);
    }

    @Override
    public Flux<DataBuffer> encode(Publisher<? extends ByteBuf> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return Flux.from(inputStream).map((byteBuffer) -> {
            return this.encodeValue(byteBuffer, bufferFactory, elementType, mimeType, hints);
        });
    }

    public DataBuffer encodeValue(ByteBuf byteBuf, DataBufferFactory bufferFactory, ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        DataBuffer dataBuffer = ((NettyDataBufferFactory) bufferFactory).wrap(byteBuf);
        if (this.logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
            String logPrefix = Hints.getLogPrefix(hints);
            this.logger.debug(logPrefix + "Writing " + dataBuffer.readableByteCount() + " bytes");
        }
        return dataBuffer;
    }
}
