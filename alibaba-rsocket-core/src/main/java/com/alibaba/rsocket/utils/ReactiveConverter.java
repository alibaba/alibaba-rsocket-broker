package com.alibaba.rsocket.utils;

import com.alibaba.rsocket.MutableContext;
import io.reactivex.*;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Reactive Converter between rxjava2 & rxjava3 and reactor
 *
 * @author linux_china
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public interface ReactiveConverter {
    List<String> REACTIVE_STREAM_CLASSES = Arrays.asList("io.reactivex.Flowable", "io.reactivex.Observable",
            "io.reactivex.rxjava3.core.Observable", "io.reactivex.rxjava3.core.Flowable", "reactor.core.publisher.Flux",
            "reactor.core.publisher.Mono","io.reactivex.Maybe","io.reactivex.Single","java.util.concurrent.CompletableFuture",
            "io.reactivex.rxjava3.core.Maybe","io.reactivex.rxjava3.core.Single","org.reactivestreams.Publisher");

    default <T> Mono<T> toMono(@Nullable Object source) {
        if (source instanceof Mono) {
            return (Mono) source;
        } else if (source instanceof CompletableFuture) {  //CompletableFuture
            return Mono.fromFuture((CompletableFuture) source);
        } else if (source instanceof Publisher) {
            return Mono.from((Publisher) source);
        } else if (source instanceof Maybe) {
            return RxJava2Adapter.maybeToMono((Maybe) source);
        } else if (source instanceof Single) {
            return RxJava2Adapter.singleToMono((Single) source);
        }
        return (Mono<T>) Mono.justOrEmpty(source);
    }


    default <T> Flux<T> toFlux(@Nullable Object source) {
        if (source == null) {
            return Flux.empty();
        } else if (source instanceof Flux) {
            return (Flux) source;
        } else if (source instanceof Iterable) {
            return Flux.fromIterable((Iterable) source);
        } else if (source instanceof Observable) {
            return (Flux<T>) RxJava2Adapter.observableToFlux((Observable) source, BackpressureStrategy.DROP);
        } else if (source instanceof Flowable) {
            return (Flux<T>) RxJava2Adapter.flowableToFlux((Flowable) source);
        } else {
            return (Flux<T>) Flux.just(source);
        }
    }

    default Object fromPublisher(Mono<?> mono, Class<?> returnType, MutableContext mutableContext) {
        if (returnType.equals(Maybe.class)) {
            return RxJava2Adapter.monoToMaybe(mono);
        } else if (returnType.equals(Single.class)) {
            return RxJava2Adapter.monoToSingle(mono);
        } else {
            return mono.subscriberContext(mutableContext::putAll);
        }
    }

    default Object fromPublisher(Flux<?> flux, Class<?> returnType, MutableContext mutableContext) {
        if (returnType.equals(Flowable.class)) {
            return RxJava2Adapter.fluxToFlowable(flux);
        } else if (returnType.equals(Observable.class)) {
            return RxJava2Adapter.fluxToObservable(flux);
        } else {
            return flux.subscriberContext(mutableContext::putAll);
        }
    }
}
