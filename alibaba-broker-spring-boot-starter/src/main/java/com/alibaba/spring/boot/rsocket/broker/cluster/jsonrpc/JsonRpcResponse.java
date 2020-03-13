package com.alibaba.spring.boot.rsocket.broker.cluster.jsonrpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

/**
 * JSON RPC response
 *
 * @author leijuan
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    @JsonProperty("id")
    private String id;
    @JsonProperty("result")
    private Object result;
    @JsonProperty("error")
    private ErrorMessage error;

    public JsonRpcResponse() {
    }

    public JsonRpcResponse(@NotNull String id, Object result) {
        this.id = id;
        this.result = result;
    }

    public JsonRpcResponse(@NotNull String id, ErrorMessage error) {
        this.id = id;
        this.error = error;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public <T> T  getResult() {
        return (T) result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public ErrorMessage getError() {
        return error;
    }

    public void setError(ErrorMessage error) {
        this.error = error;
    }
}
