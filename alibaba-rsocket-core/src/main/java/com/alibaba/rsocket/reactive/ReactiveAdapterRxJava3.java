package com.alibaba.rsocket.reactive;

import com.alibaba.rsocket.MutableContext;
import io.reactivex.rxjava3.core.*;
import org.jetbrains.annotations.Nullable;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive Adapter for RxJava3
 *
 * @author leijuan
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ReactiveAdapterRxJava3 implements ReactiveAdapter {
    private static ReactiveAdapterRxJava3 instance = new ReactiveAdapterRxJava3();

    public static ReactiveAdapterRxJava3 getInstance() {
        return instance;
    }

    @Override
    public <T> Mono<T> toMono(@Nullable Object source) {
        if (source instanceof Maybe) {
            return RxJava3Adapter.maybeToMono((Maybe) source);
        } else if (source instanceof Single) {
            return RxJava3Adapter.singleToMono((Single) source);
        } else if (source instanceof Completable) {
            return (Mono<T>) RxJava3Adapter.completableToMono((Completable) source);
        } else {
            return (Mono<T>) Mono.justOrEmpty(source);
        }
    }

    @Override
    public <T> Flux<T> toFlux(@Nullable Object source) {
        if (source instanceof Observable) {
            return (Flux<T>) RxJava3Adapter.observableToFlux((Observable) source, BackpressureStrategy.DROP);
        } else if (source instanceof Flowable) {
            return (Flux<T>) RxJava3Adapter.flowableToFlux((Flowable) source);
        } else if (source == null) {
            return Flux.empty();
        } else {
            return (Flux<T>) Flux.just(source);
        }
    }

    @Override
    public Object fromPublisher(Mono<?> mono, Class<?> returnType, MutableContext mutableContext) {
        if (returnType.equals(Maybe.class)) {
            return RxJava3Adapter.monoToMaybe(mono);
        } else if (returnType.equals(Single.class)) {
            return RxJava3Adapter.monoToSingle(mono);
        } else if (returnType.equals(Completable.class)) {
            return RxJava3Adapter.monoToCompletable(mono);
        }
        return mono;
    }

    @Override
    public Object fromPublisher(Flux<?> flux, Class<?> returnType, MutableContext mutableContext) {
        if (returnType.equals(Flowable.class)) {
            return RxJava3Adapter.fluxToFlowable(flux);
        } else if (returnType.equals(Observable.class)) {
            return RxJava3Adapter.fluxToObservable(flux);
        } else {
            return flux;
        }
    }
}
