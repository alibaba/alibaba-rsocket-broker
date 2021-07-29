package com.alibaba.rsocket.client;

import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCallerImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * RSocket Broker Connector
 *
 * @author leijuan
 */
public class RSocketBrokerConnector {
    private char[] jwtToken;
    private List<String> brokers;
    private String appName = "MockApp";
    private String topology = "intranet";
    private Map<String, String> metadata;
    private RSocketMimeType dataMimeType = RSocketMimeType.Hessian;
    LocalReactiveServiceCaller serviceCaller = new LocalReactiveServiceCallerImpl();

    private RSocketBrokerConnector() {
    }

    public static RSocketBrokerConnector create() {
        return new RSocketBrokerConnector();
    }

    public RSocketBrokerConnector jwtToken(char[] jwtToken) {
        this.jwtToken = jwtToken;
        return this;
    }

    public RSocketBrokerConnector appName(String appName) {
        this.appName = appName;
        return this;
    }

    public RSocketBrokerConnector topology(String topology) {
        this.topology = topology;
        return this;
    }

    public RSocketBrokerConnector dataMimeType(RSocketMimeType dataMimeType) {
        this.dataMimeType = Objects.requireNonNull(dataMimeType);
        return this;
    }

    public RSocketBrokerConnector service(Class<?> serviceInterface, Object handler) {
        serviceCaller.addProvider("", serviceInterface.getCanonicalName(), "", serviceInterface, handler);
        return this;
    }

    public RSocketBrokerConnector service(String serviceName, Class<?> serviceInterface, Object handler) {
        serviceCaller.addProvider("", serviceName, "", serviceInterface, handler);
        return this;
    }

    public RSocketBrokerConnector brokers(List<String> brokers) {
        this.brokers = brokers;
        return this;
    }

    public RSocketBrokerConnector metadata(String name, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(name, value);
        return this;
    }

    public RSocketBrokerClient connect() {
        return new RSocketBrokerClient(this.appName, this.brokers, topology, metadata, this.dataMimeType, this.jwtToken, this.serviceCaller);
    }

    public RSocketBrokerClient connect(List<String> brokers) {
        this.brokers = brokers;
        return connect();
    }

}
