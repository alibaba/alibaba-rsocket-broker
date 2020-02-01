package com.alibaba.rsocket.reactive;

import com.alibaba.rsocket.MutableContext;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Reactive Adapter for CompletableFuture
 *
 * @author leijuan
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ReactiveAdapterFuture implements ReactiveAdapter {
    private static ReactiveAdapterFuture instance = new ReactiveAdapterFuture();

    public static ReactiveAdapterFuture getInstance() {
        return instance;
    }

    @Override
    public <T> Mono<T> toMono(@Nullable Object source) {
        if (source == null) {
            return Mono.empty();
        } else {
            return Mono.fromFuture((CompletableFuture) source);
        }
    }

    @Override
    public <T> Flux<T> toFlux(@Nullable Object source) {
        return (Flux<T>) source;
    }

    @Override
    public Object fromPublisher(Mono<?> mono, Class<?> returnType, MutableContext mutableContext) {
        return mono.toFuture();
    }

    @Override
    public Object fromPublisher(Flux<?> flux, Class<?> returnType, MutableContext mutableContext) {
        return flux.subscriberContext(mutableContext::putAll);
    }
}
