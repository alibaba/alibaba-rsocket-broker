package com.alibaba.rsocket.rpc;

import com.alibaba.rsocket.reactive.ReactiveAdapter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * reactive method handler
 *
 * @author leijuan
 */
public class ReactiveMethodHandler {
    List<String> REACTIVE_STREAM_CLASSES = Arrays.asList("io.reactivex.Flowable", "io.reactivex.Observable",
            "io.reactivex.rxjava3.core.Observable", "io.reactivex.rxjava3.core.Flowable", "reactor.core.publisher.Flux",
            "reactor.core.publisher.Mono", "io.reactivex.Maybe", "io.reactivex.Single", "java.util.concurrent.CompletableFuture",
            "io.reactivex.rxjava3.core.Maybe", "io.reactivex.rxjava3.core.Single", "org.reactivestreams.Publisher");
    private static Map<Type, Class<?>> genericTypesCache = new ConcurrentHashMap<>();
    private Method method;
    private int parameterCount;
    private boolean asyncReturn = false;
    private ReactiveAdapter reactiveAdapter;

    public ReactiveMethodHandler(Class<?> serviceInterface, Method method) {
        this.method = method;
        this.parameterCount = method.getParameterCount();
        Class<?> returnType = this.method.getReturnType();
        if (REACTIVE_STREAM_CLASSES.contains(returnType.getCanonicalName())) {
            this.asyncReturn = true;
        }
        this.reactiveAdapter = ReactiveAdapter.findAdapter(returnType.getCanonicalName());
    }

    public Object invoke(Object obj, Object... args) throws Exception {
        return method.invoke(obj, args);
    }

    @NotNull
    public ReactiveAdapter getReactiveAdapter() {
        return reactiveAdapter;
    }

    public int getParameterCount() {
        return this.parameterCount;
    }

    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }

    public Class<?> getInferredClassForReturn() {
        return getInferredClassForGeneric(method.getGenericReturnType());
    }

    public Class<?> getInferredClassForParameter(int paramIndex) {
        return getInferredClassForGeneric(method.getGenericParameterTypes()[paramIndex]);
    }

    /**
     * get inferred class for generic type, such as Flux<T> like, please refer http://tutorials.jenkov.com/java-reflection/generics.html
     *
     * @param genericType generic type
     * @return inferred class
     */
    private static Class<?> getInferredClassForGeneric(Type genericType) {
        //performance promotion by cache
        if (!genericTypesCache.containsKey(genericType)) {
            try {
                Class<?> typeParameterClass = null;
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType type = (ParameterizedType) genericType;
                    Type[] typeArguments = type.getActualTypeArguments();
                    if (typeArguments.length > 0) {
                        typeParameterClass = (Class<?>) typeArguments[0];
                    }
                }
                if (typeParameterClass == null) {
                    typeParameterClass = (Class<?>) genericType;
                }
                genericTypesCache.put(genericType, typeParameterClass);
            } catch (Exception e) {
                return Object.class;
            }
        }
        return genericTypesCache.get(genericType);
    }

    public boolean isAsyncReturn() {
        return asyncReturn;
    }
}
