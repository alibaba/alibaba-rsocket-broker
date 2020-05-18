package com.alibaba.rsocket.reactive;

import io.netty.buffer.ByteBuf;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reactive method support
 *
 * @author leijuan
 */
public class ReactiveMethodSupport {
    public static final List<Class<?>> BINARY_CLASS_LIST = Collections.unmodifiableList(Arrays.asList(ByteBuf.class, ByteBuffer.class, byte[].class));
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
    /**
     * kotlin suspend method(coroutines)
     */
    protected Class<?> lastParamType;
    protected boolean kotlinSuspend = false;
    /**
     * reactive adapter for RxJava2 and RxJava3 etc
     */
    protected ReactiveAdapter reactiveAdapter;

    public ReactiveMethodSupport(Method method) {
        this.method = method;
        this.paramCount = method.getParameterCount();
        //result type with generic type
        this.returnType = method.getReturnType();
        this.inferredClassForReturn = parseInferredClass(method.getGenericReturnType());
        //kotlin validation
        if (paramCount > 0) {
            Type[] parameterTypes = method.getGenericParameterTypes();
            Type lastParamType = parameterTypes[paramCount - 1];
            if (lastParamType.getTypeName().startsWith("kotlin.coroutines.Continuation<")) {
                this.kotlinSuspend = true;
                if (lastParamType.getTypeName().contains("kotlin.Unit>")) {
                    this.inferredClassForReturn = Void.TYPE;
                } else {
                    this.inferredClassForReturn = parseInferredClass(lastParamType);
                }
            }
            this.lastParamType = method.getParameterTypes()[paramCount - 1];
        }
        //reactive adapter for return type
        if (this.isKotlinSuspend()) {
            this.reactiveAdapter = ReactiveAdapterKotlin.getInstance();
        } else {
            this.reactiveAdapter = ReactiveAdapter.findAdapter(returnType.getCanonicalName());
        }
    }

    public int getParamCount() {
        return paramCount;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Class<?> getInferredClassForReturn() {
        return inferredClassForReturn;
    }

    public Class<?> getLastParamType() {
        return lastParamType;
    }

    public boolean isKotlinSuspend() {
        return kotlinSuspend;
    }

    public ReactiveAdapter getReactiveAdapter() {
        return reactiveAdapter;
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
                } else if (typeArgument instanceof Class) {
                    inferredClass = (Class<?>) typeArgument;
                } else {
                    String typeName = typeArgument.getTypeName();
                    if (typeName.contains(" ")) {
                        typeName = typeName.substring(typeName.lastIndexOf(" ") + 1);
                    }
                    if (typeName.contains("<")) {
                        typeName = typeName.substring(0, typeName.indexOf("<"));
                    }
                    try {
                        inferredClass = Class.forName(typeName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (inferredClass == null && genericType instanceof Class) {
            inferredClass = (Class<?>) genericType;
        }
        return inferredClass;
    }
}
