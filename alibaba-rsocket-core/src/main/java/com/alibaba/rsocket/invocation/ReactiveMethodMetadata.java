package com.alibaba.rsocket.invocation;

import com.alibaba.rsocket.metadata.RSocketMimeType;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * reactive method metadata for service interface
 *
 * @author leijuan
 */
public class ReactiveMethodMetadata {
    private String classFullName;
    private String name;
    private int parameterCount;
    /**
     * bi directional indicate, parameter type & return type are both Flux
     */
    private boolean biDirectional = false;
    /**
     * param encoding
     */
    private RSocketMimeType paramEncoding;
    /**
     * result type
     */
    private Class<?> resultType;
    /**
     * inferred class for result type
     */
    private Class<?> inferredClassForResult;

    public ReactiveMethodMetadata(Method method, RSocketMimeType defaultEncoding) {
        this.classFullName = method.getDeclaringClass().getCanonicalName();
        this.name = method.getName();
        //result type & generic type
        this.resultType = method.getReturnType();
        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType != null) {
            //http://tutorials.jenkov.com/java-reflection/generics.html
            if (genericReturnType instanceof ParameterizedType) {
                ParameterizedType type = (ParameterizedType) genericReturnType;
                Type[] typeArguments = type.getActualTypeArguments();
                if (typeArguments.length > 0) {
                    final Type typeArgument = typeArguments[0];
                    if (typeArgument instanceof  ParameterizedType){
                        this.inferredClassForResult = (Class<?>) ((ParameterizedType) typeArgument).getActualTypeArguments()[0];
                    }else{
                        this.inferredClassForResult = (Class<?>) typeArguments[0];
                    }
                }
            }
            if (this.inferredClassForResult == null) {
                this.inferredClassForResult = (Class<?>) genericReturnType;
            }
        }
        //parameter
        this.parameterCount = method.getParameterCount();
        if (parameterCount == 1) {
            paramEncoding = defaultEncoding;
        }
        if (paramEncoding == null) {
            paramEncoding = defaultEncoding;
        }
        //bi direction check: type is Flux for 1st param or type is flux for 2nd param
        if (parameterCount == 1 && method.getParameterTypes()[0].equals(Flux.class)) {
            biDirectional = true;
        } else if (parameterCount == 2 && method.getParameterTypes()[1].equals(Flux.class)) {
            biDirectional = true;
        }

    }

    public String getClassFullName() {
        return classFullName;
    }

    public void setClassFullName(String classFullName) {
        this.classFullName = classFullName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?> getResultType() {
        return resultType;
    }

    public void setResultType(Class<?> resultType) {
        this.resultType = resultType;
    }

    public Class getInferredClassForResult() {
        return inferredClassForResult;
    }

    public void setInferredClassForResult(Class inferredClassForResult) {
        this.inferredClassForResult = inferredClassForResult;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public void setParameterCount(int parameterCount) {
        this.parameterCount = parameterCount;
    }

    public RSocketMimeType getParamEncoding() {
        return paramEncoding;
    }

    public void setParamEncoding(RSocketMimeType paramEncoding) {
        this.paramEncoding = paramEncoding;
    }

    public boolean isBiDirectional() {
        return biDirectional;
    }

    public void setBiDirectional(boolean biDirectional) {
        this.biDirectional = biDirectional;
    }

}
