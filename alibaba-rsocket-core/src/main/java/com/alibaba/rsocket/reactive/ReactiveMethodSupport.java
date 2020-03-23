package com.alibaba.rsocket.reactive;

import io.netty.buffer.ByteBuf;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reactive method support
 *
 * @author leijuan
 */
public class ReactiveMethodSupport {
    public static final List<Class<?>> BINARY_CLASS_LIST = Arrays.asList(ByteBuf.class, ByteBuffer.class, byte[].class);
    private static final Map<Type, Class<?>> genericTypesCache = new ConcurrentHashMap<>();
    protected Method method;
    protected int paramCount;
    /**
     * method's return type
     */
    protected Class<?> returnType;
    /**
     * inferred class for return type
     */
    protected Class<?> inferredClassForReturn;

    public ReactiveMethodSupport(Method method) {
        this.method = method;
        this.paramCount = method.getParameterCount();
        //result type with generic type
        this.returnType = method.getReturnType();
        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType != null) {
            this.inferredClassForReturn = parseInferredClass(genericReturnType);
        }
    }

    public int getParamCount() {
        return paramCount;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    /**
     * get inferred class for generic type, such as Flux like, please refer http://tutorials.jenkov.com/java-reflection/generics.html
     *
     * @param genericType generic type
     * @return inferred class
     */
    public static Class<?> getInferredClassForGeneric(Type genericType) {
        //performance promotion by cache
        if (!genericTypesCache.containsKey(genericType)) {
            try {
                Class<?> inferredClass = parseInferredClass(genericType);
                if (inferredClass != null) {
                    genericTypesCache.put(genericType, inferredClass);
                } else {
                    genericTypesCache.put(genericType, Object.class);
                }
            } catch (Exception e) {
                return Object.class;
            }
        }
        return genericTypesCache.get(genericType);
    }

    /**
     * get inferred class for Generic Type, please refer http://tutorials.jenkov.com/java-reflection/generics.html
     *
     * @param genericType generic type
     * @return inferred class
     */
    public static Class<?> parseInferredClass(Type genericType) {
        Class<?> inferredClass = null;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) genericType;
            Type[] typeArguments = type.getActualTypeArguments();
            if (typeArguments.length > 0) {
                final Type typeArgument = typeArguments[0];
                if (typeArgument instanceof ParameterizedType) {
                    inferredClass = (Class<?>) ((ParameterizedType) typeArgument).getActualTypeArguments()[0];
                } else {
                    inferredClass = (Class<?>) typeArgument;
                }
            }
        }
        if (inferredClass == null && genericType instanceof Class) {
            inferredClass = (Class<?>) genericType;
        }
        return inferredClass;
    }
}
