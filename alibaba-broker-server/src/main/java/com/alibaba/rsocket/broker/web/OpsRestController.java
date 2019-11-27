package com.alibaba.rsocket.broker.web;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBroker;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Ops rest controller
 *
 * @author linux_china
 */
@RestController
@RequestMapping("/ops")
public class OpsRestController {
    @Autowired
    private RSocketBrokerHandlerRegistry handlerRegistry;

    @Autowired
    private ServiceRoutingSelector serviceRoutingSelector;

    @Autowired
    private RSocketBrokerManager brokerManager;

    @RequestMapping("/services")
    public Flux<String> services() {
        return Flux.fromIterable(serviceRoutingSelector.findAllServices());
    }

    @RequestMapping("/connections")
    public Mono<Map<String, Collection<String>>> connections() {
        return Flux.fromIterable(handlerRegistry.findAll())
                .map(RSocketBrokerResponderHandler::getAppMetadata)
                .collectMultimap(AppMetadata::getName, AppMetadata::getIp);
    }

    @RequestMapping("/brokers")
    public Mono<Collection<RSocketBroker>> brokers() {
        return Mono.just(brokerManager.currentBrokers());
    }

    @PostMapping("/cluster/update")
    public Mono<Void> updateUpstream(@RequestBody String uris) throws Exception {
        final CloudEventImpl<UpstreamClusterChangedEvent> cloudEvent = getUpstreamClusterChangedEventCloudEvent(uris);
        return handlerRegistry.broadcast("*", cloudEvent);
    }

    private CloudEventImpl<UpstreamClusterChangedEvent> getUpstreamClusterChangedEventCloudEvent(@RequestBody String uris) {
        UpstreamClusterChangedEvent upstreamClusterChangedEvent = new UpstreamClusterChangedEvent("","*","",Arrays.asList(uris.split(",")));

        // passing in the given attributes
        return CloudEventBuilder.<UpstreamClusterChangedEvent>builder()
                .withType("com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent")
                .withId(UUID.randomUUID().toString())
                .withTime(ZonedDateTime.now())
                .withDataschema(URI.create("rsocket:event:com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent"))
                .withDataContentType("application/json")
                .withSource(URI.create("broker://" + RSocketAppContext.ID))
                .withData(upstreamClusterChangedEvent)
                .build();
    }
}
