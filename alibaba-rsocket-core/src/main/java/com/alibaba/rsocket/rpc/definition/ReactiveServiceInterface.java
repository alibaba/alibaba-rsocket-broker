package com.alibaba.rsocket.rpc.definition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Reactive Service interface
 *
 * @author leijuan
 */
public class ReactiveServiceInterface implements Serializable {
    private String namespace;
    /**
     * interface name
     */
    private String name;
    /**
     * service name
     */
    private String serviceName;
    private String group;
    private String version;
    private String description;
    private List<ReactiveOperation> operations = new ArrayList<>();
    private boolean deprecated;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public List<ReactiveOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<ReactiveOperation> operations) {
        this.operations = operations;
    }

    public void addOperation(ReactiveOperation operation) {
        this.operations.add(operation);
    }


}
