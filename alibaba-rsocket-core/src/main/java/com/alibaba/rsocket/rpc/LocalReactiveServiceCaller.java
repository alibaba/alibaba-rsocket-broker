package com.alibaba.rsocket.rpc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * local reactive service caller: standalone, and processor inject handler
 *
 * @author leijuan
 */
public interface LocalReactiveServiceCaller {

    /**
     * validate service and method
     *
     * @param serviceName service
     * @param rpc         rpc
     * @return legal
     */
    boolean contains(String serviceName, String rpc);

    /**
     * validate service available
     *
     * @param serviceName service name
     * @return legal
     */
    boolean contains(String serviceName);

    /**
     * validate service available
     *
     * @param serviceId service id
     * @return legal
     */
    boolean contains(Integer serviceId);

    /**
     * find all service
     *
     * @return service list
     */
    Set<String> findAllServices();

    /**
     * add service provider
     *
     * @param group group
     * @param serviceName      service name
     * @param version          version
     * @param serviceInterface interface
     * @param handler          handler
     */
    void addProvider(@NotNull String group, String serviceName, @NotNull String version, Class<?> serviceInterface, Object handler);

    /**
     * get invoke method
     *
     * @param serviceName service name
     * @param method      method  name
     * @return method object
     */
    @Nullable
    ReactiveMethodHandler getInvokeMethod(String serviceName, String method);

    /**
     * get invoke method
     *
     * @param handlerId handler id
     * @return method object
     */
    @Nullable
    ReactiveMethodHandler getInvokeMethod(Integer handlerId);

    boolean containsHandler(Integer handlerId);
}
