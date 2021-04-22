package com.alibaba.rsocket.broker.ops;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent;
import com.alibaba.spring.boot.rsocket.broker.BrokerAppContext;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBroker;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

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
    public Mono<Collection<ServiceLocator>> services() {
        return Mono.just(serviceRoutingSelector.findAllServices());
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

    @PostMapping("/broadcast/demo")
    public Mono<Void> broadcastDemo(@RequestBody Map<String, Object> content) throws Exception {
        CloudEventImpl<Map<String, Object>> cloudEvent = RSocketCloudEventBuilder.builder(content)
                .withSource(BrokerAppContext.identity())
                .withSubject("subject1")
                .build();
        return handlerRegistry.broadcast("*", cloudEvent);
    }


    @PostMapping("/stop_local_broker")
    public Mono<String> stopLocalBroker() throws Exception {
        brokerManager.stopLocalBroker();
        return Mono.just("Succeed to stop local broker from Cluster! Please shutdown app after 15 seconds!");
    }

    private CloudEventImpl<UpstreamClusterChangedEvent> getUpstreamClusterChangedEventCloudEvent(@RequestBody String uris) {
        UpstreamClusterChangedEvent upstreamClusterChangedEvent = new UpstreamClusterChangedEvent("", "*", "", Arrays.asList(uris.split(",")));

        // passing in the given attributes
        return RSocketCloudEventBuilder.builder(upstreamClusterChangedEvent)
                .withSource(BrokerAppContext.identity())
                .build();
    }
}
