package com.alibaba.rsocket.upstream;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.events.CloudEventSupport;
import com.alibaba.rsocket.events.CloudEventsConsumer;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * UpstreamClusterChangedEvent Consumer to respond the cluster changed
 *
 * @author leijuan
 */
public class UpstreamClusterChangedEventConsumer implements CloudEventsConsumer {
    private static Logger log = LoggerFactory.getLogger(UpstreamClusterChangedEventConsumer.class);
    private UpstreamManager upstreamManager;

    public UpstreamClusterChangedEventConsumer(UpstreamManager upstreamManager) {
        this.upstreamManager = upstreamManager;
    }

    @Override
    public boolean shouldAccept(CloudEventImpl<?> cloudEvent) {
        String type = cloudEvent.getAttributes().getType();
        return UpstreamClusterChangedEvent.class.getCanonicalName().equalsIgnoreCase(type);
    }

    @Override
    public Mono<Void> accept(CloudEventImpl<?> cloudEvent) {
        return Mono.fromRunnable(() -> {
            handleUpstreamClusterChangedEvent(cloudEvent);
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
}
