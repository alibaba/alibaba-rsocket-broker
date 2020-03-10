package com.alibaba.spring.boot.rsocket.broker.cluster.jsonrpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JSON RPC request
 *
 * @author leijuan
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcRequest {
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private Object params;

    @JsonProperty("id")
    private String id;

    public JsonRpcRequest() {

    }

    public JsonRpcRequest(@Nullable String method,
                          @Nullable Object params,
                          @NotNull String id) {
        this.method = method;
        this.id = id;
        this.params = params;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Request{jsonrpc=" + jsonrpc + ", method=" + method + ", id=" + id + ", params=" + params + "}";
    }
}
