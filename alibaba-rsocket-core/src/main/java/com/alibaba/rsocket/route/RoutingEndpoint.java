package com.alibaba.rsocket.route;

import com.alibaba.rsocket.metadata.GSVRoutingMetadata;

import java.util.List;

/**
 * routing endpoint
 *
 * @author leijuan
 */
public class RoutingEndpoint extends GSVRoutingMetadata {
    /**
     * uri list
     */
    private List<String> uris;

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }
}
