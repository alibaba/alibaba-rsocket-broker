package com.alibaba.rsocket.rpc.definition;

import java.io.Serializable;

/**
 * Reactive Operation parameter
 *
 * @author leijuan
 */
public class OperationParameter implements Serializable {
    private String name;
    private String type;
    private String inferredType;
    private String description;
    private boolean required;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInferredType() {
        return inferredType;
    }

    public void setInferredType(String inferredType) {
        this.inferredType = inferredType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
