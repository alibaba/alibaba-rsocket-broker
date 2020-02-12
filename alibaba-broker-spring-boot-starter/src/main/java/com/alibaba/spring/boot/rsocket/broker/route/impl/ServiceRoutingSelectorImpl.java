package com.alibaba.spring.boot.rsocket.broker.route.impl;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.utils.MurmurHash3;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * service routine selector implementation
 *
 * @author leijuan
 */
public class ServiceRoutingSelectorImpl implements ServiceRoutingSelector {
    /**
     * service to instances bitmap
     */
    private IntObjectHashMap<RoaringBitmap> servicesBitmap = new IntObjectHashMap<>();
    /**
     * distinct Services
     */
    private IntObjectHashMap<ServiceLocator> distinctServices = new IntObjectHashMap<>();

    /**
     * instance to services
     */
    private FastListMultimap<Integer, Integer> instanceServices = new FastListMultimap<>();

    @Override
    public void register(Integer instanceId, Set<ServiceLocator> services) {
        if (instanceServices.containsKey(instanceId)) {
            return;
        }
        for (ServiceLocator serviceLocator : services) {
            int serviceId = serviceLocator.getId();
            instanceServices.put(instanceId, serviceId);
            if (!servicesBitmap.containsKey(serviceId)) {
                servicesBitmap.put(serviceId, new RoaringBitmap());
            }
            servicesBitmap.get(serviceId).add(instanceId);
            distinctServices.put(serviceId, serviceLocator);
        }
    }

    @Override
    public void deregister(Integer instanceId) {
        if (instanceServices.containsKey(instanceId)) {
            for (Integer serviceId : instanceServices.get(instanceId)) {
                RoaringBitmap bitmap = servicesBitmap.get(serviceId);
                if (bitmap != null) {
                    bitmap.remove(instanceId);
                    if (bitmap.getCardinality() == 0) {
                        servicesBitmap.remove(serviceId);
                        distinctServices.remove(serviceId);
                    }
                }
            }
        }
    }

    @Override
    public boolean containInstance(Integer instanceId) {
        return instanceServices.containsKey(instanceId);
    }

    @Override
    public boolean containService(Integer serviceId) {
        return servicesBitmap.containsKey(serviceId);
    }

    @Override
    public @Nullable ServiceLocator findServiceById(Integer serviceId) {
        return distinctServices.get(serviceId);
    }

    @Nullable
    @Override
    public Integer findHandler(Integer serviceId) {
        RoaringBitmap bitmap = servicesBitmap.get(serviceId);
        if (bitmap != null) {
            int cardinality = bitmap.getCardinality();
            if (cardinality > 0) {
                return bitmap.select(ThreadLocalRandom.current().nextInt(cardinality));
            }
        }
        return null;
    }

    @Override
    public Collection<Integer> findHandlers(Integer serviceId) {
        RoaringBitmap bitmap = servicesBitmap.get(serviceId);
        if (bitmap != null) {
            List<Integer> ids = new ArrayList<>();
            bitmap.forEach((IntConsumer) ids::add);
            return ids;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Integer getInstanceCount(Integer serviceId) {
        RoaringBitmap bitmap = servicesBitmap.get(serviceId);
        if (bitmap != null) {
            return bitmap.getCardinality();
        }
        return 0;
    }

    @Override
    public Integer getInstanceCount(String serviceName) {
        return getInstanceCount(MurmurHash3.hash32(serviceName));
    }

    @Override
    public Collection<ServiceLocator> findAllServices() {
        return distinctServices.values();
    }
}
