package com.alibaba.rsocket.events;

import java.util.List;

/**
 * Invalid Cache Event
 *
 * @author leijuan
 */
public class InvalidCacheEvent implements CloudEventSupport<InvalidCacheEvent> {
    /**
     * cache keys
     */
    private List<String> keys;

    public InvalidCacheEvent(List<String> keys) {
        this.keys = keys;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }
}
