package com.alibaba.rsocket.reactive;

import com.alibaba.rsocket.MutableContext;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

/**
 * Reactive Adapter default
 *
 * @author leijuan
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ReactiveAdapterDefault implements ReactiveAdapter {
    private static ReactiveAdapterDefault instance = new ReactiveAdapterDefault();

    public static ReactiveAdapterDefault getInstance() {
        return instance;
    }

    @Override
    public <T> Mono<T> toMono(@Nullable Object source) {
        if (source instanceof Mono) {
            return (Mono) source;
        } else if (source instanceof Publisher) {
            return Mono.from((Publisher) source);
        } else {
            return (Mono<T>) Mono.justOrEmpty(source);
        }
    }

    @Override
    public <T> Flux<T> toFlux(@Nullable Object source) {
        if (source instanceof Flux) {
            return (Flux) source;
        } else if (source instanceof Iterable) {
            return Flux.fromIterable((Iterable) source);
        } else if (source instanceof Stream) {
            return Flux.fromStream((Stream) source);
        } else if (source instanceof Publisher) {
            return Flux.from((Publisher) source);
        } else if (source == null) {
            return Flux.empty();
        } else if (source.getClass().isArray()) {
            return Flux.fromArray((T[]) source);
        }
        return (Flux<T>) Flux.just(source);
    }

    @Override
    public Object fromPublisher(Mono<?> mono, Class<?> returnType, MutableContext mutableContext) {
        return mono.subscriberContext(mutableContext::putAll);
    }

    @Override
    public Object fromPublisher(Flux<?> flux, Class<?> returnType, MutableContext mutableContext) {
        return flux.subscriberContext(mutableContext::putAll);
    }

    @Override
    public Object fromPublisher(Flux<?> flux, Class<?> returnType) {
        return flux;
    }
}
