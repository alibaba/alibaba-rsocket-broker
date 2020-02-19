package com.alibaba.spring.boot.rsocket.broker.cluster;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.events.CloudEventSupport;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.route.RSocketFilter;
import com.alibaba.rsocket.transport.NetworkUtil;
import com.alibaba.spring.boot.rsocket.broker.events.AppConfigEvent;
import com.alibaba.spring.boot.rsocket.broker.events.RSocketFilterEnableEvent;
import com.alibaba.spring.boot.rsocket.broker.services.ConfigurationService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cloudevents.json.Json;
import io.cloudevents.v1.CloudEventImpl;
import io.scalecube.cluster.Cluster;
import io.scalecube.cluster.ClusterImpl;
import io.scalecube.cluster.ClusterMessageHandler;
import io.scalecube.cluster.Member;
import io.scalecube.cluster.membership.MembershipEvent;
import io.scalecube.cluster.transport.api.Message;
import io.scalecube.net.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.alibaba.rsocket.cloudevents.CloudEventRSocket.CLOUD_EVENT_TYPE_REFERENCE;

/**
 * RSocket Broker Manager Gossip implementation
 *
 * @author leijuan
 */
public class RSocketBrokerManagerGossipImpl implements RSocketBrokerManager, ClusterMessageHandler {
    private Logger log = LoggerFactory.getLogger(RSocketBrokerManagerGossipImpl.class);
    /**
     * Gossip listen port
     */
    private static int gossipListenPort = 42254;
    /**
     * seed members
     */
    @Value("${rsocket.broker.seeds}")
    private String[] seeds;
    @Autowired
    private ApplicationContext applicationContext;

    private Cluster cluster;
    private RSocketBroker localBroker;
    /**
     * rsocket brokers, key is ip address
     */
    private Map<String, RSocketBroker> brokers = new HashMap<>();
    /**
     * brokers changes emitter processor
     */
    private EmitterProcessor<Collection<RSocketBroker>> brokersEmitterProcessor = EmitterProcessor.create();

    @PostConstruct
    public void init() {
        final String localIp = NetworkUtil.getLocalIP();
        cluster = new ClusterImpl()
                .config(clusterConfig -> clusterConfig.containerHost(localIp).containerPort(gossipListenPort))
                .membership(membershipConfig -> membershipConfig.seedMembers(seedMembers()).syncInterval(5_000))
                .transport(transportConfig -> transportConfig.host(localIp).port(gossipListenPort))
                .handler(cluster1 -> this)
                .startAwait();
        brokers.put(localIp, new RSocketBroker(localIp));
        this.localBroker = new RSocketBroker(localIp);
        log.info("Start cluster with Gossip support!");
    }

    @Override
    public Flux<Collection<RSocketBroker>> requestAll() {
        return brokersEmitterProcessor;
    }

    @Override
    public Collection<RSocketBroker> currentBrokers() {
        return this.brokers.values();
    }

    @Override
    public RSocketBroker localBroker() {
        return this.localBroker;
    }

    @Override
    public Mono<RSocketBroker> findByIp(String ip) {
        if (brokers.containsKey(ip)) {
            return Mono.just(this.brokers.get(ip));
        } else {
            return Mono.empty();
        }
    }

    @Override
    public Flux<ServiceLocator> findServices(String ip) {
        return Flux.empty();
    }

    @Override
    public Boolean isStandAlone() {
        return false;
    }

    public List<Address> seedMembers() {
        return Stream.of(seeds)
                .map(host -> Address.create(host, gossipListenPort))
                .collect(Collectors.toList());
    }

    @Override
    public void onMessage(Message message) {
        //peer to peer cluster.send()
    }

    @Override
    public void onGossip(Message gossip) {
        if (gossip.header("cloudevents") != null) {
            onCloudEvent(Json.decodeValue(gossip.data(), CLOUD_EVENT_TYPE_REFERENCE));
        }
    }

    public void onCloudEvent(CloudEventImpl<ObjectNode> cloudEvent) {
        String type = cloudEvent.getAttributes().getType();
        if (RSocketFilterEnableEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
            //filter enable event
            RSocketFilterEnableEvent filterEnableEvent = CloudEventSupport.unwrapData(cloudEvent, RSocketFilterEnableEvent.class);
            if (filterEnableEvent != null) {
                try {
                    RSocketFilter rsocketFilter = (RSocketFilter) applicationContext.getBean(Class.forName(filterEnableEvent.getFilterClassName()));
                    rsocketFilter.setEnabled(filterEnableEvent.isEnabled());
                } catch (Exception ignore) {

                }
            }
        } else if (AppConfigEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
            AppConfigEvent appConfigEvent = CloudEventSupport.unwrapData(cloudEvent, AppConfigEvent.class);
            if (appConfigEvent != null) {
                ConfigurationService configurationService = applicationContext.getBean(ConfigurationService.class);
                configurationService.put(appConfigEvent.getAppName() + "." + appConfigEvent.getKey(), appConfigEvent.getVale());
            }
        }
    }

    @Override
    public Mono<String> spread(CloudEventImpl<?> cloudEvent) {
        Message message = Message.builder()
                .header("cloudevents", "true")
                .data(Json.encode(cloudEvent))
                .build();
        return cluster.spreadGossip(message);
    }

    @Override
    public void onMembershipEvent(MembershipEvent event) {
        RSocketBroker broker = memberToBroker(event.member());
        if (event.isAdded()) {
            brokers.put(broker.getIp(), broker);
            log.info(RsocketErrorCode.message("RST-300001", broker.getIp(), "added"));
        } else if (event.isRemoved()) {
            brokers.remove(broker.getIp());
            log.info(RsocketErrorCode.message("RST-300001", broker.getIp(), "removed"));
        } else if (event.isLeaving()) {
            brokers.remove(broker.getIp());
            log.info(RsocketErrorCode.message("RST-300001", broker.getIp(), "left"));
        }
        brokersEmitterProcessor.onNext(brokers.values());
    }

    private RSocketBroker memberToBroker(Member member) {
        RSocketBroker broker = new RSocketBroker();
        broker.setIp(member.address().host());
        return broker;
    }

    @Override
    public void stopLocalBroker() {
        this.cluster.shutdown();
    }
}
