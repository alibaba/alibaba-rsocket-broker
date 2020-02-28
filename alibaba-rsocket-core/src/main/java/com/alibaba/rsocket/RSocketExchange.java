package com.alibaba.rsocket;

import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import io.rsocket.Payload;
import io.rsocket.frame.FrameType;

import java.util.HashMap;
import java.util.Map;

/**
 * RSocket exchange
 *
 * @author leijuan
 */
public class RSocketExchange {
    private FrameType frameType;
    /**
     * routing metadata
     */
    private GSVRoutingMetadata routingMetadata;
    /**
     * source app
     */
    private AppMetadata source;
    /**
     * request payload
     */
    private Payload payload;
    private Map<Object, Object> attributes = new HashMap<>();

    public RSocketExchange() {
    }

    public RSocketExchange(FrameType frameType, GSVRoutingMetadata routingMetadata, Payload payload, AppMetadata source) {
        this.frameType = frameType;
        this.routingMetadata = routingMetadata;
        this.payload = payload;
        this.source = source;
    }

    public FrameType getFrameType() {
        return frameType;
    }

    public GSVRoutingMetadata getRoutingMetadata() {
        return routingMetadata;
    }

    public Payload getPayload() {
        return payload;
    }

    public AppMetadata getSource() {
        return source;
    }

    public Map<Object, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<Object, Object> attributes) {
        this.attributes = attributes;
    }
}
