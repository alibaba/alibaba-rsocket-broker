package com.alibaba.rsocket.discovery;

import java.util.Map;

/**
 * service instance
 *
 * @author leijuan
 */
public interface ServiceInstance {
    /**
     * @return The unique instance ID as registered.
     */
    String getInstanceId();

    /**
     * @return The service ID as registered.
     */
    String getServiceId();

    /**
     * @return The hostname of the registered service instance.
     */
    String getHost();

    /**
     * @return The port of the registered service instance.
     */
    int getPort();

    /**
     * @return Whether the port of the registered service instance uses HTTPS.
     */
    boolean isSecure();

    /**
     * @return The service URI address.
     */
    String getUri();

    /**
     * @return The key / value pair metadata associated with the service instance.
     */
    Map<String, String> getMetadata();

    /**
     * @return The scheme of the service instance.
     */
    String getScheme();
}
