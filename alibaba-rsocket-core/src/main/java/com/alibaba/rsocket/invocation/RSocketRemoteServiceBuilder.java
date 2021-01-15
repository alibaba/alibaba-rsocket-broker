package com.alibaba.rsocket.invocation;

import brave.Tracing;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.ServiceMapping;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamManager;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.net.URI;
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
    private URI sourceUri;
    private String group;
    private String service;
    private String version;
    private Duration timeout = Duration.ofMillis(3000);
    private String endpoint;
    private boolean sticky;
    private Class<T> serviceInterface;
    private RSocketMimeType encodingType = RSocketMimeType.Hessian;
    private RSocketMimeType acceptEncodingType;
    private UpstreamCluster upstreamCluster;
    /**
     * zipkin brave tracing client
     */
    private boolean braveTracing = true;
    private Tracing tracing;
    public static boolean byteBuddyAvailable = true;

    static {
        try {
            Class.forName("net.bytebuddy.ByteBuddy");
        } catch (Exception e) {
            byteBuddyAvailable = false;
        }
    }

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
            builder.sticky = serviceMapping.sticky();
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

    public RSocketRemoteServiceBuilder<T> sticky(boolean sticky) {
        this.sticky = sticky;
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

    /**
     * GraalVM nativeImage support: set encodeType and acceptEncodingType to Json
     * @return  this
     */
    public RSocketRemoteServiceBuilder<T> nativeImage() {
        this.encodingType = RSocketMimeType.Json;
        this.acceptEncodingType = RSocketMimeType.Json;
        return this;
    }

    public RSocketRemoteServiceBuilder<T> upstreamManager(UpstreamManager upstreamManager) {
        String serviceId = ServiceLocator.serviceId(group, service, version);
        UpstreamCluster upstream = upstreamManager.findClusterByServiceId(serviceId);
        if (upstream == null) {
            upstream = upstreamManager.findBroker();
        }
        this.upstreamCluster = upstream;
        this.sourceUri = upstreamManager.requesterSupport().originUri();
        return this;
    }

    public T build() {
        if (byteBuddyAvailable) {
            return buildByteBuddyProxy();
        } else {
            return buildJdkProxy();
        }
    }

    @NotNull
    private RSocketRequesterRpcProxy getRequesterProxy() {
        if (this.braveTracing && this.tracing != null) {
            return new RSocketRequesterRpcZipkinProxy(tracing, upstreamCluster, group, serviceInterface, service, version,
                    encodingType, acceptEncodingType, timeout, endpoint, sticky, sourceUri, !byteBuddyAvailable);
        } else {
            return new RSocketRequesterRpcProxy(upstreamCluster, group, serviceInterface, service, version,
                    encodingType, acceptEncodingType, timeout, endpoint, sticky, sourceUri, !byteBuddyAvailable);
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

    public T buildByteBuddyProxy() {
        CONSUMED_SERVICES.add(new ServiceLocator(group, service, version));
        RSocketRequesterRpcProxy proxy = getRequesterProxy();
        return ByteBuddyUtils.build(this.serviceInterface, proxy);
    }
}
