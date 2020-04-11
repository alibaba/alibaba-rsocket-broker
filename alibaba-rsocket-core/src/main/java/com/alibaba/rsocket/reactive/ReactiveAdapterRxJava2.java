package com.alibaba.rsocket.reactive;

import com.alibaba.rsocket.MutableContext;
import io.reactivex.*;
import org.jetbrains.annotations.Nullable;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive Adapter for RxJava2
 *
 * @author leijuan
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ReactiveAdapterRxJava2 implements ReactiveAdapter {
    private static ReactiveAdapterRxJava2 instance = new ReactiveAdapterRxJava2();

    public static ReactiveAdapterRxJava2 getInstance() {
        return instance;
    }

    @Override
    public <T> Mono<T> toMono(@Nullable Object source) {
        if (source instanceof Maybe) {
            return RxJava2Adapter.maybeToMono((Maybe) source);
        } else if (source instanceof Single) {
            return RxJava2Adapter.singleToMono((Single) source);
        } else if (source instanceof Completable) {
            return (Mono<T>) RxJava2Adapter.completableToMono((Completable) source);
        } else {
            return (Mono<T>) Mono.justOrEmpty(source);
        }
    }

    @Override
    public <T> Flux<T> toFlux(@Nullable Object source) {
        if (source instanceof Observable) {
            return (Flux<T>) RxJava2Adapter.observableToFlux((Observable) source, BackpressureStrategy.BUFFER);
        } else if (source instanceof Flowable) {
            return (Flux<T>) RxJava2Adapter.flowableToFlux((Flowable) source);
        } else if (source == null) {
            return Flux.empty();
        } else {
            return (Flux<T>) Flux.just(source);
        }

    }

    @Override
    public Object fromPublisher(Mono<?> mono, Class<?> returnType, MutableContext mutableContext) {
        if (returnType.equals(Maybe.class)) {
            return RxJava2Adapter.monoToMaybe(mono);
        } else if (returnType.equals(Single.class)) {
            return RxJava2Adapter.monoToSingle(mono);
        } else if (returnType.equals(Completable.class)) {
            return RxJava2Adapter.monoToCompletable(mono);
        }
        return mono;
    }

    @Override
    public Object fromPublisher(Flux<?> flux, Class<?> returnType, MutableContext mutableContext) {
        return fromPublisher(flux, returnType);
    }

    @Override
    public Object fromPublisher(Flux<?> flux, Class<?> returnType) {
        if (returnType.equals(Flowable.class)) {
            return RxJava2Adapter.fluxToFlowable(flux);
        } else if (returnType.equals(Observable.class)) {
            return RxJava2Adapter.fluxToObservable(flux);
        } else {
            return flux;
        }
    }
}

