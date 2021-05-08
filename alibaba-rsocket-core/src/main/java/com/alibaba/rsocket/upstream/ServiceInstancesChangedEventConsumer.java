package com.alibaba.rsocket.upstream;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.events.CloudEventSupport;
import com.alibaba.rsocket.events.CloudEventsConsumer;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * ServiceInstancesChangedEvent consumer
 *
 * @author leijuan
 */
public class ServiceInstancesChangedEventConsumer implements CloudEventsConsumer {
    private static Logger log = LoggerFactory.getLogger(ServiceInstancesChangedEventConsumer.class);
    private UpstreamManager upstreamManager;

    public ServiceInstancesChangedEventConsumer(UpstreamManager upstreamManager) {
        this.upstreamManager = upstreamManager;
    }

    @Override
    public boolean shouldAccept(CloudEventImpl<?> cloudEvent) {
        String type = cloudEvent.getAttributes().getType();
        return ServiceInstancesChangedEvent.class.getCanonicalName().equalsIgnoreCase(type);
    }

    @Override
    public Mono<Void> accept(CloudEventImpl<?> cloudEvent) {
        return Mono.fromRunnable(() -> {
            handleServicesChangedEvent(cloudEvent);
        });
    }

    public void handleServicesChangedEvent(CloudEventImpl<?> cloudEvent) {
        ServiceInstancesChangedEvent serviceInstancesChangedEvent = CloudEventSupport.unwrapData(cloudEvent, ServiceInstancesChangedEvent.class);
        if (serviceInstancesChangedEvent != null) {
            String serviceId = ServiceLocator.serviceId(serviceInstancesChangedEvent.getGroup(), serviceInstancesChangedEvent.getService(), serviceInstancesChangedEvent.getVersion());
            final UpstreamCluster upstreamCluster = upstreamManager.findClusterByServiceId(serviceId);
            if (upstreamCluster != null) {
                if (serviceInstancesChangedEvent.getUris().isEmpty()) { //remove upstream cluster
                    this.upstreamManager.remove(upstreamCluster);
                    Mono.delay(Duration.ofSeconds(60)).subscribe(timestamp -> {
                        try {
                            upstreamCluster.close();
                        } catch (Exception ignore) {

                        }
                    });
                } else { // update upstream cluster's uris
                    upstreamCluster.setUris(serviceInstancesChangedEvent.getUris());
                }
                log.info(RsocketErrorCode.message("RST-300202", serviceId, String.join(",", serviceInstancesChangedEvent.getUris())));
            } else {
                if (!serviceInstancesChangedEvent.getUris().isEmpty()) {
                    try {
                        UpstreamCluster newUpstreamCluster = new UpstreamCluster(serviceInstancesChangedEvent.getGroup(),
                                serviceInstancesChangedEvent.getService(),
                                serviceInstancesChangedEvent.getVersion(),
                                serviceInstancesChangedEvent.getUris());
                        newUpstreamCluster.setRsocketAware(upstreamManager.requesterSupport());
                        newUpstreamCluster.init();
                        upstreamManager.add(newUpstreamCluster);
                        log.info(RsocketErrorCode.message("RST-300202", serviceId, String.join(",", serviceInstancesChangedEvent.getUris())));
                    } catch (Exception e) {
                        log.error(RsocketErrorCode.message("RST-400500", String.join(",", serviceInstancesChangedEvent.getUris())), e);
                    }
                }
            }
        }
    }
}
