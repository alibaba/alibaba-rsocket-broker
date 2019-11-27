package com.alibaba.rsocket.route;

/**
 * service not found exception
 *
 * @author leijuan
 */
public class ServiceNotFoundException extends Exception {

    public ServiceNotFoundException(String serviceId) {
        super(serviceId);
    }
}
