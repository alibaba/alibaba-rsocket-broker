package com.alibaba.rsocket;

import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.route.RSocketRequestType;
import io.rsocket.Payload;

import java.util.HashMap;
import java.util.Map;

/**
 * RSocket exchange
 *
 * @author leijuan
 */
public class RSocketExchange {
    private RSocketRequestType type;
    private GSVRoutingMetadata routingMetadata;
    private Payload payload;
    private Map<Object, Object> attributes = new HashMap<>();

    public RSocketExchange() {
    }

    public RSocketExchange(RSocketRequestType type, GSVRoutingMetadata routingMetadata, Payload payload) {
        this.type = type;
        this.routingMetadata = routingMetadata;
        this.payload = payload;
    }

    public RSocketRequestType getType() {
        return type;
    }

    public void setType(RSocketRequestType type) {
        this.type = type;
    }

    public GSVRoutingMetadata getRoutingMetadata() {
        return routingMetadata;
    }

    public void setRoutingMetadata(GSVRoutingMetadata routingMetadata) {
        this.routingMetadata = routingMetadata;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public Map<Object, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<Object, Object> attributes) {
        this.attributes = attributes;
    }
}
