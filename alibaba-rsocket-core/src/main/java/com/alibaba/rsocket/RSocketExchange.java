package com.alibaba.rsocket;

import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
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
    private RSocketCompositeMetadata compositeMetadata;
    private Payload payload;
    private Map<Object, Object> attributes = new HashMap<>();

    public RSocketExchange() {
    }

    public RSocketExchange(FrameType frameType, RSocketCompositeMetadata compositeMetadata, Payload payload) {
        this.frameType = frameType;
        this.compositeMetadata = compositeMetadata;
        this.payload = payload;
    }

    public FrameType getFrameType() {
        return frameType;
    }

    public void setFrameType(FrameType frameType) {
        this.frameType = frameType;
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
