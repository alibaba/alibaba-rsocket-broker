package com.alibaba.rsocket.rpc;

import com.alibaba.rsocket.ServiceMapping;
import com.alibaba.rsocket.utils.MurmurHash3;
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
    private Map<Integer, Object> rsocketHashCodeServices = new HashMap<>();
    private Map<String, ReactiveMethodHandler> methodInvokeEntrances = new HashMap<>();
    private Map<Integer, ReactiveMethodHandler> methodHashCodeInvokeEntrances = new HashMap<>();

    @Override
    public boolean contains(String serviceName, String rpc) {
        return methodInvokeEntrances.containsKey(serviceName + "." + rpc);
    }

    @Override
    public boolean contains(Integer serviceId) {
        return rsocketHashCodeServices.containsKey(serviceId);
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
        rsocketHashCodeServices.put(MurmurHash3.hash32(serviceName), handler);
        for (Method method : serviceInterface.getMethods()) {
            String handlerName = method.getName();
            ServiceMapping serviceMapping = method.getAnnotation(ServiceMapping.class);
            if (serviceMapping != null && !serviceMapping.value().isEmpty()) {
                handlerName = serviceMapping.value();
            }
            String key = serviceName + "." + handlerName;
            methodInvokeEntrances.put(key, new ReactiveMethodHandler(serviceInterface, method, handler));
            methodHashCodeInvokeEntrances.put(MurmurHash3.hash32(key), new ReactiveMethodHandler(serviceInterface, method, handler));
        }
    }

    @Override
    public @Nullable ReactiveMethodHandler getInvokeMethod(String serviceName, String method) {
        return methodInvokeEntrances.get(serviceName + "." + method);
    }

    @Override
    public @Nullable ReactiveMethodHandler getInvokeMethod(Integer handlerId) {
        return methodHashCodeInvokeEntrances.get(handlerId);
    }

    @Override
    public boolean containsHandler(Integer handlerId) {
        return methodHashCodeInvokeEntrances.containsKey(handlerId);
    }
}
