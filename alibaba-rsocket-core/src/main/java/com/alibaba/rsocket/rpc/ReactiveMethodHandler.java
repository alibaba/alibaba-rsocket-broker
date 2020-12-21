package com.alibaba.rsocket.rpc;

import com.alibaba.rsocket.reactive.ReactiveMethodSupport;

import java.lang.reflect.Method;

import static com.alibaba.rsocket.constants.ReactiveStreamConstants.REACTIVE_STREAM_CLASSES;

/**
 * reactive method handler
 *
 * @author leijuan
 */
public class ReactiveMethodHandler extends ReactiveMethodSupport {

    private Object handler;
    private String serviceName;
    private boolean asyncReturn = false;
    private boolean binaryReturn;
    private Class<?>[] parametersType;

    public ReactiveMethodHandler(String serviceName, Method method, Object handler) {
        super(method);
        this.handler = handler;
        this.serviceName = serviceName;
        this.method = method;
        this.method.setAccessible(true);
        this.parametersType = this.method.getParameterTypes();
        if (kotlinSuspend || REACTIVE_STREAM_CLASSES.contains(this.returnType.getCanonicalName())) {
            this.asyncReturn = true;
        }
        this.binaryReturn = this.inferredClassForReturn != null && BINARY_CLASS_LIST.contains(this.inferredClassForReturn);
    }

    public Object invoke(Object... args) throws Exception {
        if (kotlinSuspend) {
            return CoroutinesKt.suspendCallToMono(handler, method, args);
        } else {
            return method.invoke(this.handler, args);
        }
    }

    public Class<?>[] getParameterTypes() {
        return this.parametersType;
    }

    public void setParametersType(Class<?>[] parametersType) {
        this.parametersType = parametersType;
    }

    public Class<?> getInferredClassForParameter(int paramIndex) {
        return ReactiveMethodSupport.getInferredClassForGeneric(method.getGenericParameterTypes()[paramIndex]);
    }

    public boolean isAsyncReturn() {
        return asyncReturn;
    }

    public void setAsyncReturn(boolean asyncReturn) {
        this.asyncReturn = asyncReturn;
    }

    public boolean isBinaryReturn() {
        return this.binaryReturn;
    }

    public String getServiceName() {
        return serviceName;
    }
}
