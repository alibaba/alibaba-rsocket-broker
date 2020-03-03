package com.alibaba.spring.boot.rsocket.hessian;

import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Hessian Decoder
 *
 * @author leijuan
 */
public class HessianDecoder extends HessianCodecSupport implements Decoder<Object> {

    @Override
    public boolean canDecode(@NotNull ResolvableType elementType, MimeType mimeType) {
        return HESSIAN_MIME_TYPE.equals(mimeType);
    }

    @NotNull
    @Override
    public Flux<Object> decode(@NotNull Publisher<DataBuffer> inputStream, @NotNull ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return Flux.from(inputStream).handle(((dataBuffer, sink) -> {
            try {
                sink.next(decode(dataBuffer));
            } catch (Exception e) {
                sink.error(e);
            }
        }));
    }

    @NotNull
    @Override
    public Mono<Object> decodeToMono(@NotNull Publisher<DataBuffer> inputStream, @NotNull ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return Mono.from(inputStream).handle((dataBuffer, sink) -> {
            try {
                sink.next(decode(dataBuffer));
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    @NotNull
    @Override
    public List<MimeType> getDecodableMimeTypes() {
        return HESSIAN_MIME_TYPES;
    }
}
