package com.alibaba.spring.boot.rsocket.broker.route;

import com.alibaba.rsocket.ServiceLocator;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * service routing selector: find handler according to service id
 *
 * @author leijuan
 */
public interface ServiceRoutingSelector {

    void register(Integer instanceId, Set<ServiceLocator> services);

    void register(Integer instanceId, int powerUnit, Set<ServiceLocator> services);

    Set<Integer> findServicesByInstance(Integer instanceId);

    void deregister(Integer instanceId);

    void deregister(Integer instanceId, Integer serviceId);

    void registerP2pServiceConsumer(Integer instanceId, List<String> p2pServices);

    void unRegisterP2pServiceConsumer(Integer instanceId, List<String> p2pServices);

    List<Integer> findP2pServiceConsumers(String p2pService);

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
