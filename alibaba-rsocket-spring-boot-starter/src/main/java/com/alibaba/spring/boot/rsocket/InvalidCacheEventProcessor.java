package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.events.CloudEventSupport;
import com.alibaba.rsocket.events.InvalidCacheEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import reactor.extra.processor.TopicProcessor;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * InvalidCacheEvent processor
 *
 * @author leijuan
 */
public class InvalidCacheEventProcessor {
    @Autowired(required = false)
    private CacheManager cacheManager;
    @Autowired
    @Qualifier("reactiveCloudEventProcessor")
    private TopicProcessor<CloudEventImpl> eventProcessor;

    @PostConstruct
    public void init() {
        eventProcessor.subscribe(cloudEvent -> {
            String type = cloudEvent.getAttributes().getType();
            if (InvalidCacheEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
                handleInvalidCache(cloudEvent);
            }
        });
    }

    public void handleInvalidCache(CloudEventImpl<?> cloudEvent) {
        if (cacheManager == null) return;
        InvalidCacheEvent invalidCacheEvent = CloudEventSupport.unwrapData(cloudEvent, InvalidCacheEvent.class);
        if (invalidCacheEvent != null) {
            invalidateSpringCache(invalidCacheEvent.getKeys());
        }
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
