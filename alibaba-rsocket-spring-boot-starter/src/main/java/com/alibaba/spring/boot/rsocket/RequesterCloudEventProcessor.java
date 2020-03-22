package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.events.CloudEventSupport;
import com.alibaba.rsocket.events.InvalidCacheEvent;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent;
import com.alibaba.rsocket.upstream.UpstreamManager;
import io.cloudevents.v1.CloudEventImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import reactor.extra.processor.TopicProcessor;

import java.util.List;

/**
 * RSocket requester cloud event processor
 *
 * @author leijuan
 */
public class RequesterCloudEventProcessor {
    private static final Logger log = LoggerFactory.getLogger(RequesterCloudEventProcessor.class);
    @Autowired
    private UpstreamManager upstreamManager;
    @Autowired(required = false)
    private CacheManager cacheManager;
    @Autowired
    @Qualifier("reactiveCloudEventProcessor")
    private TopicProcessor<CloudEventImpl> eventProcessor;

    public void init() {
        eventProcessor.subscribe(cloudEvent -> {
            String type = cloudEvent.getAttributes().getType();
            if (UpstreamClusterChangedEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
                handleUpstreamClusterChangedEvent(cloudEvent);
            } else if (InvalidCacheEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
                handleInvalidCache(cloudEvent);
            } else {
                log.info(RsocketErrorCode.message("RST-610501", type));
            }
        });
    }

    public void handleUpstreamClusterChangedEvent(CloudEventImpl<?> cloudEvent) {
        UpstreamClusterChangedEvent clusterChangedEvent = CloudEventSupport.unwrapData(cloudEvent, UpstreamClusterChangedEvent.class);
        if (clusterChangedEvent != null) {
            String serviceId = ServiceLocator.serviceId(clusterChangedEvent.getGroup(), clusterChangedEvent.getInterfaceName(), clusterChangedEvent.getVersion());
            UpstreamCluster upstreamCluster = upstreamManager.findClusterByServiceId(serviceId);
            if (upstreamCluster != null) {
                upstreamCluster.setUris(clusterChangedEvent.getUris());
                log.info(RsocketErrorCode.message("RST-300202", serviceId, String.join(",", clusterChangedEvent.getUris())));
            }
        }
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
