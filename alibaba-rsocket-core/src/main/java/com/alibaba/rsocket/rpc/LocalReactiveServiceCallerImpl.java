package com.alibaba.rsocket.rpc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * local reactive service caller implementation
 *
 * @author leijuan
 */
public class LocalReactiveServiceCallerImpl implements LocalReactiveServiceCaller {
    private Map<String, Object> rsocketServices = new HashMap<>();
    private Map<String, ReactiveMethodHandler> methodInvokeEntrances = new HashMap<>();

    @Override
    public Object invoke(String serviceName, String rpc, Object... args) throws Exception {
        return methodInvokeEntrances.get(serviceName + "." + rpc).invoke(rsocketServices.get(serviceName), args);
    }

    @Override
    public boolean contains(String serviceName, String rpc) {
        return methodInvokeEntrances.containsKey(serviceName + "." + rpc);
    }

    @Override
    public boolean contains(String serviceName) {
        return rsocketServices.containsKey(serviceName);
    }

    @Override
    public Set<String> findAllServices() {
        return rsocketServices.keySet();
    }

    public void addProvider(@NotNull String group, String serviceName, @NotNull String version, Class<?> serviceInterface, Object handler) {
        rsocketServices.put(serviceName, handler);
        for (Method method : serviceInterface.getMethods()) {
            methodInvokeEntrances.put(serviceName + "." + method.getName(), new ReactiveMethodHandler(serviceInterface, method));
        }
    }

    @Override
    public @Nullable ReactiveMethodHandler getInvokeMethod(String serviceName, String method) {
        return methodInvokeEntrances.get(serviceName + "." + method);
    }
}
