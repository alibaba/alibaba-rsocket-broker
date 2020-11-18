package com.alibaba.broker.example;

import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.rpc.ReactiveMethodHandler;
import com.alibaba.rsocket.utils.MurmurHash3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * local Reactive Cloud function caller
 *
 * @author leijuan
 */
public class LocalReactiveFunctionCaller implements LocalReactiveServiceCaller {
    private Set<String> functionServices;
    private Set<String> functionNames;
    private Set<Integer> rsocketHashCodeServices = new HashSet<>();
    private Map<String, ReactiveMethodHandler> methodInvokeEntrances = new HashMap<>();
    private Map<Integer, ReactiveMethodHandler> methodHashCodeInvokeEntrances = new HashMap<>();

    public LocalReactiveFunctionCaller(FunctionRegistry functionRegistry) {
        this.functionNames = functionRegistry.getNames(null).stream()
                .filter(functionName -> functionName.contains("."))
                .collect(Collectors.toSet());
        this.functionServices = this.functionNames.stream()
                .map(functionName -> functionName.substring(0, functionName.lastIndexOf(".")))
                .collect(Collectors.toSet());
        for (String functionService : this.functionServices) {
            rsocketHashCodeServices.add(MurmurHash3.hash32(functionService));
        }
        for (String functionName : this.functionNames) {
            SimpleFunctionRegistry.FunctionInvocationWrapper function = functionRegistry.lookup(functionName);
            try {
                Method method = function.getClass().getMethod("apply", Object.class);
                String serviceName = functionName.substring(0, functionName.lastIndexOf("."));
                ReactiveMethodHandler reactiveMethodHandler = new ReactiveMethodHandler(serviceName, method, function);
                String functionTypeName = function.getFunctionType().getTypeName();
                if (functionTypeName.contains(Mono.class.getCanonicalName()) || functionTypeName.contains(Flux.class.getCanonicalName())) {
                    reactiveMethodHandler.setAsyncReturn(true);
                }
                if (functionTypeName.startsWith(Function.class.getCanonicalName())) {
                    String paramTypeName = functionTypeName.substring(functionTypeName.indexOf("<") + 1, functionTypeName.indexOf(","));
                    reactiveMethodHandler.setParametersType(new Class[]{Class.forName(paramTypeName)});
                }
                methodInvokeEntrances.put(functionName, reactiveMethodHandler);
                methodHashCodeInvokeEntrances.put(MurmurHash3.hash32(functionName), reactiveMethodHandler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean contains(String serviceName, String rpc) {
        return functionNames.contains(serviceName + "." + rpc);
    }

    @Override
    public boolean contains(String serviceName) {
        return functionServices.contains(serviceName);
    }

    @Override
    public boolean contains(Integer serviceId) {
        return rsocketHashCodeServices.contains(serviceId);
    }

    @Override
    public Set<String> findAllServices() {
        return functionServices;
    }

    @Override
    public void addProvider(@NotNull String group, String serviceName, @NotNull String version, Class<?> serviceInterface, Object handler) {
        throw new RuntimeException("Custom service registration not support!");
    }

    @Override
    public void removeProvider(@NotNull String group, String serviceName, @NotNull String version, Class<?> serviceInterface) {

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
