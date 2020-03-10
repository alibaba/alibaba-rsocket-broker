package com.alibaba.spring.boot.rsocket.broker.cluster.jsonrpc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON RPC error message
 *
 * @author leijuan
 */
public class ErrorMessage {
    @JsonProperty("code")
    private final int code;
    @JsonProperty("message")
    private final String message;

    public ErrorMessage(@JsonProperty("code") int code,
                        @JsonProperty("message") String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
