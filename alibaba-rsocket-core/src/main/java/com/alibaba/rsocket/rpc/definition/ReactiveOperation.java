package com.alibaba.rsocket.rpc.definition;

import java.util.ArrayList;
import java.util.List;

/**
 * Reactive Operation
 *
 * @author leijuan
 */
public class ReactiveOperation {
    private String name;
    private String description;
    private boolean deprecated;
    private String returnType;
    private String inferredType;
    private List<Parameter> parameters = new ArrayList<>();

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

    public String getInferredType() {
        return inferredType;
    }

    public void setInferredType(String inferredType) {
        this.inferredType = inferredType;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(Parameter param) {
        this.parameters.add(param);
    }
}
