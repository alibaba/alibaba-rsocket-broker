package com.alibaba.rsocket.rpc;

import io.reactivex.Maybe;
import io.reactivex.Single;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * reactive method handler
 *
 * @author leijuan
 */
public class ReactiveMethodHandler {
    private static Map<Type, Class<?>> genericTypesCache = new ConcurrentHashMap<>();
    private Method method;
    private int parameterCount;
    private boolean asyncReturn = false;

    public ReactiveMethodHandler(Class<?> serviceInterface, Method method) {
        this.method = method;
        this.parameterCount = method.getParameterCount();
        Class<?> returnType = this.method.getReturnType();
        if (returnType.isAssignableFrom(Publisher.class)
                || returnType.isAssignableFrom(CompletableFuture.class)
                || returnType.isAssignableFrom(Single.class)
                || returnType.isAssignableFrom(Maybe.class)) {
            this.asyncReturn = true;
        }
    }

    public Object invoke(Object obj, Object... args) throws Exception {
        return method.invoke(obj, args);
    }

    public int getParameterCount() {
        return this.parameterCount;
    }

    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }

    public Class<?> getInferredClassForResult() {
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
