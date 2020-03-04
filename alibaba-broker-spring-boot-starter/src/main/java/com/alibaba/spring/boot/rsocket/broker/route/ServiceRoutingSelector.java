package com.alibaba.spring.boot.rsocket.broker.route;

import com.alibaba.rsocket.ServiceLocator;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * service routing selector: find handler according to service id
 *
 * @author leijuan
 */
public interface ServiceRoutingSelector {

    void register(Integer instanceId, Set<ServiceLocator> services);

    void register(Integer instanceId, int powerUnit, Set<ServiceLocator> services);

    void deregister(Integer instanceId);

    boolean containInstance(Integer instanceId);

    boolean containService(Integer serviceId);

    @Nullable
    ServiceLocator findServiceById(Integer serviceId);

    @Nullable
    Integer findHandler(Integer serviceId);

    Collection<Integer> findHandlers(Integer serviceId);

    Integer getInstanceCount(Integer serviceId);

    Integer getInstanceCount(String serviceName);

    Collection<ServiceLocator> findAllServices();

    int getDistinctServiceCount();
}
