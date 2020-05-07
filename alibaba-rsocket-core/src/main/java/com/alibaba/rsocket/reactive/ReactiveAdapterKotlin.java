package com.alibaba.rsocket.reactive;

import com.alibaba.rsocket.MutableContext;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.reactive.ReactiveFlowKt;
import kotlinx.coroutines.reactor.ReactorFlowKt;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive Adapter for Kotlin
 *
 * @author leijuan
 */
public class ReactiveAdapterKotlin implements ReactiveAdapter {
    private static ReactiveAdapterKotlin instance = new ReactiveAdapterKotlin();

    public static ReactiveAdapterKotlin getInstance() {
        return instance;
    }

    @Override
    public <T> Mono<T> toMono(@Nullable Object source) {
        return null;
    }

    @Override
    public <T> Flux<T> toFlux(@Nullable Object source) {
        if (source == null) {
            return Flux.empty();
        } else {
            //noinspection unchecked
            return ReactorFlowKt.asFlux((Flow<T>) source);
        }
    }

    @Override
    public Object fromPublisher(Mono<?> mono, Class<?> returnType, MutableContext mutableContext) {
        final Continuation<?> continuation = mutableContext.get(Continuation.class);
        mono.doOnSuccess(continuation::resumeWith).doOnError(continuation::resumeWith).subscribe();
        return IntrinsicsKt.getCOROUTINE_SUSPENDED();
    }

    @Override
    public Object fromPublisher(Flux<?> flux, Class<?> returnType, MutableContext mutableContext) {
        return ReactiveFlowKt.asFlow(flux);
    }

    @Override
    public Object fromPublisher(Flux<?> flux, Class<?> returnType) {
        return ReactiveFlowKt.asFlow(flux);
    }
}
