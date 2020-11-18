package com.alibaba.rsocket.rpc;

import com.alibaba.rsocket.ServiceMapping;
import com.alibaba.rsocket.reactive.ReactiveMethodSupport;
import com.alibaba.rsocket.rpc.definition.OperationParameter;
import com.alibaba.rsocket.rpc.definition.ReactiveOperation;
import com.alibaba.rsocket.rpc.definition.ReactiveServiceInterface;
import com.alibaba.rsocket.utils.MurmurHash3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * local reactive service caller implementation
 *
 * @author leijuan
 */
public class LocalReactiveServiceCallerImpl implements LocalReactiveServiceCaller, ReactiveServiceDiscovery {
    private Map<String, Object> rsocketServices = new HashMap<>();
    private Map<String, ReactiveServiceInterface> reactiveServiceInterfaces = new HashMap<>();
    private Map<Integer, Object> rsocketHashCodeServices = new HashMap<>();
    private Map<String, ReactiveMethodHandler> methodInvokeEntrances = new HashMap<>();
    private Map<Integer, ReactiveMethodHandler> methodHashCodeInvokeEntrances = new HashMap<>();

    public LocalReactiveServiceCallerImpl() {
        //add ReactiveServiceDiscovery
        addProvider("", ReactiveServiceDiscovery.class.getCanonicalName(), "", ReactiveServiceDiscovery.class, this);
    }

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
        reactiveServiceInterfaces.put(serviceName, createReactiveServiceInterface(group, version, serviceName, serviceInterface));
        rsocketHashCodeServices.put(MurmurHash3.hash32(serviceName), handler);
        for (Method method : serviceInterface.getMethods()) {
            if (!method.isDefault()) {
                String handlerName = method.getName();
                ServiceMapping serviceMapping = method.getAnnotation(ServiceMapping.class);
                if (serviceMapping != null && !serviceMapping.value().isEmpty()) {
                    handlerName = serviceMapping.value();
                }
                String key = serviceName + "." + handlerName;
                methodInvokeEntrances.put(key, new ReactiveMethodHandler(serviceName, method, handler));
                methodHashCodeInvokeEntrances.put(MurmurHash3.hash32(key), new ReactiveMethodHandler(serviceName, method, handler));
            }
        }
    }

    @Override
    public void removeProvider(@NotNull String group, String serviceName, @NotNull String version, Class<?> serviceInterface) {
        rsocketServices.remove(serviceName);
        reactiveServiceInterfaces.remove(serviceName);
        rsocketHashCodeServices.remove(MurmurHash3.hash32(serviceName));
        for (Method method : serviceInterface.getMethods()) {
            if (!method.isDefault()) {
                String handlerName = method.getName();
                ServiceMapping serviceMapping = method.getAnnotation(ServiceMapping.class);
                if (serviceMapping != null && !serviceMapping.value().isEmpty()) {
                    handlerName = serviceMapping.value();
                }
                String key = serviceName + "." + handlerName;
                methodInvokeEntrances.remove(key);
                methodHashCodeInvokeEntrances.remove(MurmurHash3.hash32(key));
            }
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

    public ReactiveServiceInterface createReactiveServiceInterface(String group, String version,
                                                                   String serviceFullName, Class<?> javaInterface) {
        ReactiveServiceInterface serviceInterface = new ReactiveServiceInterface();
        serviceInterface.setGroup(group);
        serviceInterface.setVersion(version);
        if (javaInterface.getPackage() != null) {
            serviceInterface.setNamespace(javaInterface.getPackage().getName());
        }
        serviceInterface.setName(javaInterface.getSimpleName());
        serviceInterface.setServiceName(serviceFullName);
        Deprecated interfaceDeprecated = javaInterface.getAnnotation(Deprecated.class);
        if (interfaceDeprecated != null) {
            serviceInterface.setDeprecated(true);
        }
        for (Method method : javaInterface.getMethods()) {
            if (!method.isDefault()) {
                String handlerName = method.getName();
                ReactiveOperation operation = new ReactiveOperation();
                Deprecated methodDeprecated = method.getAnnotation(Deprecated.class);
                if (methodDeprecated != null) {
                    operation.setDeprecated(true);
                }
                serviceInterface.addOperation(operation);
                operation.setName(handlerName);
                operation.setReturnType(method.getReturnType().getCanonicalName());
                operation.setReturnInferredType(ReactiveMethodSupport.parseInferredClass(method.getGenericReturnType()).getCanonicalName());
                for (Parameter parameter : method.getParameters()) {
                    OperationParameter param = new OperationParameter();
                    operation.addParameter(param);
                    param.setName(parameter.getName());
                    param.setType(parameter.getType().getCanonicalName());
                    String inferredType = ReactiveMethodSupport.parseInferredClass(parameter.getParameterizedType()).getCanonicalName();
                    if (!param.getType().equals(inferredType)) {
                        param.setInferredType(inferredType);
                    }
                }
            }
        }
        return serviceInterface;
    }

    @Override
    public ReactiveServiceInterface findServiceByFullName(String serviceFullName) {
        return this.reactiveServiceInterfaces.get(serviceFullName);
    }
}
