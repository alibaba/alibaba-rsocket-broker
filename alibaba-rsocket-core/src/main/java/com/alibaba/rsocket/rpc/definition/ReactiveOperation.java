package com.alibaba.rsocket.rpc.definition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Reactive Operation
 *
 * @author leijuan
 */
public class ReactiveOperation implements Serializable {
    private String name;
    private String description;
    private boolean deprecated;
    private String returnType;
    private String returnInferredType;
    private List<OperationParameter> parameters = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getReturnInferredType() {
        return returnInferredType;
    }

    public void setReturnInferredType(String returnInferredType) {
        this.returnInferredType = returnInferredType;
    }

    public List<OperationParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<OperationParameter> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(OperationParameter param) {
        this.parameters.add(param);
    }
}
