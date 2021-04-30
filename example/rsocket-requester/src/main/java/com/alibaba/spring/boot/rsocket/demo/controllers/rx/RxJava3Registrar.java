package com.alibaba.spring.boot.rsocket.demo.controllers.rx;

import io.reactivex.rxjava3.core.*;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ReactiveTypeDescriptor;

/**
 * Rxjava3 Registrar
 *
 * @author leijuan
 */
public class RxJava3Registrar {

    public void registerAdapters(ReactiveAdapterRegistry registry) {
        registry.registerReactiveType(
                ReactiveTypeDescriptor.multiValue(Flowable.class, Flowable::empty),
                source -> (Flowable<?>) source,
                Flowable::fromPublisher
        );
        registry.registerReactiveType(
                ReactiveTypeDescriptor.multiValue(Observable.class, Observable::empty),
                source -> ((Observable<?>) source).toFlowable(BackpressureStrategy.BUFFER),
                source -> Flowable.fromPublisher(source).toObservable()
        );
        registry.registerReactiveType(
                ReactiveTypeDescriptor.singleRequiredValue(Single.class),
                source -> ((Single<?>) source).toFlowable(),
                source -> Flowable.fromPublisher(source).toObservable().singleElement().toSingle()
        );
        registry.registerReactiveType(
                ReactiveTypeDescriptor.singleOptionalValue(Maybe.class, Maybe::empty),
                source -> ((Maybe<?>) source).toFlowable(),
                source -> Flowable.fromPublisher(source).toObservable().singleElement()
        );
        registry.registerReactiveType(
                ReactiveTypeDescriptor.noValue(Completable.class, Completable::complete),
                source -> ((Completable) source).toFlowable(),
                source -> Flowable.fromPublisher(source).toObservable().ignoreElements()
        );
    }
}
