package com.alibaba.rsocket.rpc;

import com.alibaba.rsocket.rpc.definition.ReactiveServiceInterface;

/**
 * Reactive Service Discovery
 *
 * @author leijuan
 */
public interface ReactiveServiceDiscovery {

    ReactiveServiceInterface findServiceByFullName(String serviceFullName);
}
