package com.alibaba.rsocket.invocation;

import brave.Tracing;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.ServiceMapping;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamManager;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.jetbrains.annotations.NotNull;

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
    /**
     * zipkin brave tracing client
     */
    private boolean braveTracing = true;
    private Tracing tracing;

    public static <T> RSocketRemoteServiceBuilder<T> client(Class<T> serviceInterface) {
        RSocketRemoteServiceBuilder<T> builder = new RSocketRemoteServiceBuilder<T>();
        builder.serviceInterface = serviceInterface;
        builder.service = serviceInterface.getCanonicalName();
        ServiceMapping serviceMapping = serviceInterface.getAnnotation(ServiceMapping.class);
        if (serviceMapping != null) {
            if (!serviceMapping.group().isEmpty()) {
                builder.group = serviceMapping.group();
            }
            if (!serviceMapping.version().isEmpty()) {
                builder.version = serviceMapping.group();
            }
            if (!serviceMapping.value().isEmpty()) {
                builder.service = serviceMapping.value();
            }
            if (!serviceMapping.endpoint().isEmpty()) {
                builder.endpoint = serviceMapping.endpoint();
            }
            if (!serviceMapping.paramEncoding().isEmpty()) {
                builder.encodingType = RSocketMimeType.valueOfType(serviceMapping.paramEncoding());
            }
            if (!serviceMapping.resultEncoding().isEmpty()) {
                builder.acceptEncodingType = RSocketMimeType.valueOfType(serviceMapping.resultEncoding());
            }
        }
        try {
            Class.forName("brave.propagation.TraceContext");
        } catch (ClassNotFoundException e) {
            builder.braveTracing = false;
        }
        return builder;
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

    public RSocketRemoteServiceBuilder<T> tracing(Tracing tracing) {
        this.tracing = tracing;
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
        RSocketRequesterRpcProxy proxy = getRequesterProxy();
        Class<T> dynamicType = (Class<T>) new ByteBuddy(ClassFileVersion.JAVA_V8)
                .subclass(serviceInterface)
                .name(serviceInterface.getSimpleName() + "RSocketStub")
                .method(ElementMatchers.not(ElementMatchers.isDefaultMethod()))
                .intercept(MethodDelegation.to(proxy))
                .make()
                .load(serviceInterface.getClassLoader())
                .getLoaded();
        try {
            return dynamicType.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private RSocketRequesterRpcProxy getRequesterProxy() {
        if (this.braveTracing && this.tracing != null) {
            return new RSocketRequesterRpcZipkinProxy(tracing, upstreamCluster, group, serviceInterface, service, version,
                    encodingType, acceptEncodingType, timeout, endpoint);
        } else {
            return new RSocketRequesterRpcProxy(upstreamCluster, group, serviceInterface, service, version,
                    encodingType, acceptEncodingType, timeout, endpoint);
        }
    }

    @SuppressWarnings("unchecked")
    public T buildJdkProxy() {
        CONSUMED_SERVICES.add(new ServiceLocator(group, service, version));
        RSocketRequesterRpcProxy proxy = getRequesterProxy();
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class[]{serviceInterface},
                proxy);
    }
}
