package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.events.CloudEventSupport;
import com.alibaba.rsocket.events.CloudEventsConsumer;
import com.alibaba.rsocket.events.InvalidCacheEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * InvalidCacheEvent consumer
 *
 * @author leijuan
 */
public class InvalidCacheEventConsumer implements CloudEventsConsumer {
    @Autowired(required = false)
    private CacheManager cacheManager;

    @Override
    public boolean shouldAccept(CloudEventImpl<?> cloudEvent) {
        String type = cloudEvent.getAttributes().getType();
        return cacheManager != null && InvalidCacheEvent.class.getCanonicalName().equalsIgnoreCase(type);
    }

    @Override
    public Mono<Void> accept(CloudEventImpl<?> cloudEvent) {
        return Mono.fromRunnable(() -> {
            InvalidCacheEvent invalidCacheEvent = CloudEventSupport.unwrapData(cloudEvent, InvalidCacheEvent.class);
            if (invalidCacheEvent != null) {
                invalidateSpringCache(invalidCacheEvent.getKeys());
            }
        });
    }

    private void invalidateSpringCache(List<String> keys) {
        if (cacheManager == null) return;
        keys.forEach(key -> {
            String[] parts = key.split(":", 2);
            try {
                Cache cache = cacheManager.getCache(parts[0]);
                if (cache != null) {
                    cache.evict(parts[1]);
                }
            } catch (Exception ignore) {

            }
        });
    }

}
