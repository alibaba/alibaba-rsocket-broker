package com.alibaba.rsocket.constants;

import java.util.Arrays;
import java.util.List;

/**
 * Reactive Stream Constants
 *
 * @author kevinten10
 */
public interface ReactiveStreamConstants {

    /**
     * The {@code Reactive Stream} steaming classes.
     * see {@code io.reactivex}, {@code reactor.core} and {@code kotlinx.coroutines.flow}
     */
    List<String> REACTIVE_STREAMING_CLASSES = Arrays.asList(
            "io.reactivex.Flowable",
            "io.reactivex.Observable",
            "io.reactivex.rxjava3.core.Observable",
            "io.reactivex.rxjava3.core.Flowable",
            "reactor.core.publisher.Flux",
            "kotlinx.coroutines.flow.Flow");

    /**
     * The {@code Reactive Stream} related classes.
     * see {@code io.reactivex}, {@code reactor.core}, {@code org.reactivestreams} and {@code Jdk}
     */
    List<String> REACTIVE_STREAM_CLASSES = Arrays.asList(
            "io.reactivex.Maybe",
            "io.reactivex.Single",
            "io.reactivex.Completable",
            "io.reactivex.Flowable",
            "io.reactivex.Observable",
            "io.reactivex.rxjava3.core.Observable",
            "io.reactivex.rxjava3.core.Flowable",
            "io.reactivex.rxjava3.core.Maybe",
            "io.reactivex.rxjava3.core.Single",
            "io.reactivex.rxjava3.core.Completable",
            "reactor.core.publisher.Mono",
            "reactor.core.publisher.Flux",
            "org.reactivestreams.Publisher",
            "java.util.concurrent.CompletableFuture");
}
