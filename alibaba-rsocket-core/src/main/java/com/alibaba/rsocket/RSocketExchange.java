package com.alibaba.rsocket;

import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
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
    private RSocketCompositeMetadata compositeMetadata;
    private Payload payload;
    private Map<Object, Object> attributes = new HashMap<>();

    public RSocketExchange() {
    }

    public RSocketExchange(RSocketRequestType type, RSocketCompositeMetadata compositeMetadata, Payload payload) {
        this.type = type;
        this.compositeMetadata = compositeMetadata;
        this.payload = payload;
    }

    public RSocketRequestType getType() {
        return type;
    }

    public void setType(RSocketRequestType type) {
        this.type = type;
    }

    public RSocketCompositeMetadata getCompositeMetadata() {
        return compositeMetadata;
    }

    public void setCompositeMetadata(RSocketCompositeMetadata compositeMetadata) {
        this.compositeMetadata = compositeMetadata;
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
