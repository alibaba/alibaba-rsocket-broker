package com.alibaba.rsocket.invocation;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamManager;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * RSocket remote service builder
 *
 * @author leijuan
 */
public class RSocketRemoteServiceBuilder<T> {
    public static final Set<ServiceLocator> CONSUMED_SERVICES = new HashSet<>();
    private String group;
    private String service;
    private String version;
    private Duration timeout = Duration.ofMillis(3000);
    private String endpoint;
    private Class<T> serviceInterface;
    private RSocketMimeType encodingType = RSocketMimeType.Hessian;
    private RSocketMimeType acceptEncodingType;
    private UpstreamCluster upstreamCluster;

    public static <T> RSocketRemoteServiceBuilder<T> client(Class<T> serviceInterface) {
        RSocketRemoteServiceBuilder<T> rSocketServiceBuilder = new RSocketRemoteServiceBuilder<T>();
        rSocketServiceBuilder.serviceInterface = serviceInterface;
        rSocketServiceBuilder.service = serviceInterface.getCanonicalName();
        return rSocketServiceBuilder;
    }

    public RSocketRemoteServiceBuilder<T> group(String group) {
        this.group = group;
        return this;
    }

    public RSocketRemoteServiceBuilder<T> service(String service) {
        this.service = service;
        return this;
    }

    public RSocketRemoteServiceBuilder<T> version(String version) {
        this.version = version;
        return this;
    }

    /**
     * timeout configuration, and default timeout is 3000 millis
     * if the call is long time task, please set it to big value
     *
     * @param millis millis
     * @return builder
     */
    public RSocketRemoteServiceBuilder<T> timeoutMillis(int millis) {
        this.timeout = Duration.ofMillis(millis);
        return this;
    }

    public RSocketRemoteServiceBuilder<T> endpoint(String endpoint) {
        assert endpoint.contains(":");
        this.endpoint = endpoint;
        return this;
    }

    public RSocketRemoteServiceBuilder<T> upstream(UpstreamCluster upstreamCluster) {
        this.upstreamCluster = upstreamCluster;
        return this;
    }

    public RSocketRemoteServiceBuilder<T> encodingType(RSocketMimeType encodingType) {
        this.encodingType = encodingType;
        return this;
    }

    public RSocketRemoteServiceBuilder<T> acceptEncodingType(RSocketMimeType encodingType) {
        this.acceptEncodingType = encodingType;
        return this;
    }

    public RSocketRemoteServiceBuilder<T> upstreamManager(UpstreamManager upstreamManager) {
        String serviceId = ServiceLocator.serviceId(group, service, version);
        UpstreamCluster upstream = upstreamManager.findClusterByServiceId(serviceId);
        if (upstream == null) {
            upstream = upstreamManager.findBroker();
        }
        this.upstreamCluster = upstream;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T build() {
        CONSUMED_SERVICES.add(new ServiceLocator(group, service, version));
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class[]{serviceInterface},
                new RSocketRequesterRpcProxy(upstreamCluster, group, serviceInterface, service, version,
                        encodingType, acceptEncodingType, timeout, endpoint));
    }
}
