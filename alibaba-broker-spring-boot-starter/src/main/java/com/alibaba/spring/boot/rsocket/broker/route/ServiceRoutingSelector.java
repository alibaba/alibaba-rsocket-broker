package com.alibaba.spring.boot.rsocket.broker.route;

import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Set;

/**
 * service routing selector: find handler according service id
 *
 * @author leijuan
 */
public interface ServiceRoutingSelector {

    void register(Integer instanceId, Set<String> services);

    void deregister(Integer instanceId);

    boolean containInstance(Integer instanceId);

    boolean containService(Integer serviceId);

    @Nullable
    Integer findHandler(Integer serviceId);

    Collection<Integer> findHandlers(Integer serviceId);

    Integer getInstanceCount(Integer serviceId);

    Integer getInstanceCount(String serviceName);

    Collection<String> findAllServices();
}
