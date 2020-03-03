package com.alibaba.spring.boot.rsocket.hessian;

import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Hessian encoder
 *
 * @author leijuan
 */
public class HessianEncoder extends HessianCodecSupport implements Encoder<Object> {

    @Override
    public boolean canEncode(@NotNull ResolvableType elementType, MimeType mimeType) {
        return HESSIAN_MIME_TYPE.equals(mimeType);
    }

    @NotNull
    @Override
    public Flux<DataBuffer> encode(@NotNull Publisher<?> inputStream, @NotNull DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return Flux.from(inputStream)
                .handle((obj, sink) -> {
                    try {
                        sink.next(encode(obj, bufferFactory));
                    } catch (Exception e) {
                        sink.error(e);
                    }
                });
    }

    @NotNull
    @Override
    public DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory, ResolvableType valueType, MimeType mimeType, Map<String, Object> hints) {
        if (value == null) {
            return bufferFactory.allocateBuffer(0);
        }
        try {
            return encode(value, bufferFactory);
        } catch (Exception e) {
            return bufferFactory.allocateBuffer(0);
        }
    }

    @NotNull
    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return HESSIAN_MIME_TYPES;
    }
}
