package com.alibaba.rsocket.rpc;

import com.alibaba.rsocket.reactive.ReactiveAdapter;
import com.alibaba.rsocket.reactive.ReactiveMethodSupport;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * reactive method handler
 *
 * @author leijuan
 */
public class ReactiveMethodHandler extends ReactiveMethodSupport {
    List<String> REACTIVE_STREAM_CLASSES = Arrays.asList("io.reactivex.Flowable", "io.reactivex.Observable",
            "io.reactivex.rxjava3.core.Observable", "io.reactivex.rxjava3.core.Flowable", "reactor.core.publisher.Flux",
            "reactor.core.publisher.Mono", "io.reactivex.Maybe", "io.reactivex.Single", "io.reactivex.Completable", "java.util.concurrent.CompletableFuture",
            "io.reactivex.rxjava3.core.Maybe", "io.reactivex.rxjava3.core.Single", "io.reactivex.rxjava3.core.Completable", "org.reactivestreams.Publisher");
    private Object handler;
    private boolean asyncReturn = false;
    private boolean binaryReturn;
    private ReactiveAdapter reactiveAdapter;

    public ReactiveMethodHandler(Class<?> serviceInterface, Method method, Object handler) {
        super(method);
        this.handler = handler;
        this.method = method;
        this.method.setAccessible(true);
        if (REACTIVE_STREAM_CLASSES.contains(this.returnType.getCanonicalName())) {
            this.asyncReturn = true;
        }
        this.binaryReturn = this.inferredClassForReturn != null && BINARY_CLASS_LIST.contains(this.inferredClassForReturn);
        this.reactiveAdapter = ReactiveAdapter.findAdapter(returnType.getCanonicalName());
    }

    public Object invoke(Object... args) throws Exception {
        return method.invoke(this.handler, args);
    }

    @NotNull
    public ReactiveAdapter getReactiveAdapter() {
        return reactiveAdapter;
    }

    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }

    public Class<?> getInferredClassForParameter(int paramIndex) {
        return ReactiveMethodSupport.getInferredClassForGeneric(method.getGenericParameterTypes()[paramIndex]);
    }

    public boolean isAsyncReturn() {
        return asyncReturn;
    }

    public boolean isBinaryReturn() {
        return this.binaryReturn;
    }
}
