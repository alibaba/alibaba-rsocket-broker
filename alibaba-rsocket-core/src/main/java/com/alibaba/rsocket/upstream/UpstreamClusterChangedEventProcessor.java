package com.alibaba.rsocket.upstream;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.events.CloudEventSupport;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.extra.processor.TopicProcessor;

/**
 * UpstreamClusterChangedEvent Processor to respond the cluster changed
 *
 * @author leijuan
 */
public class UpstreamClusterChangedEventProcessor {
    private static Logger log = LoggerFactory.getLogger(UpstreamClusterChangedEventProcessor.class);
    private UpstreamManager upstreamManager;
    private TopicProcessor<CloudEventImpl> eventProcessor;

    public UpstreamClusterChangedEventProcessor(UpstreamManager upstreamManager, TopicProcessor<CloudEventImpl> eventProcessor) {
        this.upstreamManager = upstreamManager;
        this.eventProcessor = eventProcessor;
    }

    public void init() {
        eventProcessor.subscribe(cloudEvent -> {
            String type = cloudEvent.getAttributes().getType();
            if (UpstreamClusterChangedEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
                handleUpstreamClusterChangedEvent(cloudEvent);
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
}
