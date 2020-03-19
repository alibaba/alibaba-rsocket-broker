package com.alibaba.spring.boot.rsocket.broker.cluster;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.route.RSocketFilter;
import com.alibaba.rsocket.transport.NetworkUtil;
import com.alibaba.spring.boot.rsocket.broker.RSocketBrokerProperties;
import com.alibaba.spring.boot.rsocket.broker.cluster.jsonrpc.JsonRpcRequest;
import com.alibaba.spring.boot.rsocket.broker.cluster.jsonrpc.JsonRpcResponse;
import com.alibaba.spring.boot.rsocket.broker.events.AppConfigEvent;
import com.alibaba.spring.boot.rsocket.broker.events.RSocketFilterEnableEvent;
import com.alibaba.spring.boot.rsocket.broker.services.ConfigurationService;
import io.cloudevents.v1.CloudEventImpl;
import io.micrometer.core.instrument.Metrics;
import io.scalecube.cluster.Cluster;
import io.scalecube.cluster.ClusterImpl;
import io.scalecube.cluster.ClusterMessageHandler;
import io.scalecube.cluster.Member;
import io.scalecube.cluster.membership.MembershipEvent;
import io.scalecube.cluster.transport.api.Message;
import io.scalecube.net.Address;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RSocket Broker Manager Gossip implementation
 *
 * @author leijuan
 */
public class RSocketBrokerManagerGossipImpl implements RSocketBrokerManager, ClusterMessageHandler, DisposableBean {
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
    @Autowired
    private RSocketBrokerProperties brokerProperties;

    private Mono<Cluster> monoCluster;
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
        final String localIp = NetworkUtil.LOCAL_IP;
        monoCluster = new ClusterImpl()
                .config(clusterConfig -> clusterConfig.containerHost(localIp).containerPort(gossipListenPort))
                .membership(membershipConfig -> membershipConfig.seedMembers(seedMembers()).syncInterval(5_000))
                .transport(transportConfig -> transportConfig.host(localIp).port(gossipListenPort))
                .handler(cluster1 -> this)
                .start();
        //subscribe and start & join the cluster
        monoCluster.subscribe();
        this.localBroker = new RSocketBroker(localIp, brokerProperties.getExternalDomain());
        brokers.put(localIp, localBroker);
        log.info(RsocketErrorCode.message("RST-300002"));
        Metrics.globalRegistry.gauge("cluster.broker.count", this, (DoubleFunction<RSocketBrokerManagerGossipImpl>) brokerManagerGossip -> brokerManagerGossip.brokers.size());
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
        if (message.header("jsonrpc") != null) {
            JsonRpcRequest request = message.data();
            Message replyMessage = Message.builder()
                    .correlationId(message.correlationId())
                    .data(OnJsonRpcCall(request))
                    .build();
            this.monoCluster.flatMap(cluster -> cluster.send(message.sender(), replyMessage)).subscribe();
        }
    }

    public JsonRpcResponse OnJsonRpcCall(JsonRpcRequest request) {
        Object result;
        if (request.getMethod().equals("BrokerService.getConfiguration")) {
            Map<String, String> config = new HashMap<>();
            config.put("rsocket.broker.externalDomain", brokerProperties.getExternalDomain());
            result = config;
        } else {
            result = "";
        }
        return new JsonRpcResponse(request.getId(), result);
    }

    public Mono<JsonRpcResponse> makeJsonRpcCall(@NotNull Member member, @NotNull String methodName, @Nullable Object params) {
        String uuid = UUID.randomUUID().toString();
        Message jsonRpcMessage = Message.builder()
                .correlationId(uuid)
                .header("jsonrpc", "2.0")
                .data(new JsonRpcRequest(methodName, params, uuid))
                .build();
        return monoCluster.flatMap(cluster -> cluster.requestResponse(member, jsonRpcMessage)).map(Message::data);
    }

    @Override
    public void onGossip(Message gossip) {
        if (gossip.header("cloudevents") != null) {
            onCloudEvent(gossip.data());
        }
    }

    public void onCloudEvent(CloudEventImpl<Object> cloudEvent) {
        Optional<Object> cloudEventData = cloudEvent.getData();
        cloudEventData.ifPresent(data -> {
            if (data instanceof RSocketFilterEnableEvent) {
                try {
                    RSocketFilterEnableEvent filterEnableEvent = (RSocketFilterEnableEvent) data;
                    RSocketFilter rsocketFilter = (RSocketFilter) applicationContext.getBean(Class.forName(filterEnableEvent.getFilterClassName()));
                    rsocketFilter.setEnabled(filterEnableEvent.isEnabled());
                } catch (Exception ignore) {

                }
            } else if (data instanceof AppConfigEvent) {
                AppConfigEvent appConfigEvent = (AppConfigEvent) data;
                ConfigurationService configurationService = applicationContext.getBean(ConfigurationService.class);
                configurationService.put(appConfigEvent.getAppName() + ":" + appConfigEvent.getKey(), appConfigEvent.getVale()).subscribe();
            }
        });
    }

    @Override
    public Mono<String> broadcast(CloudEventImpl<?> cloudEvent) {
        Message message = Message.builder()
                .header("cloudevents", "true")
                .data(cloudEvent)
                .build();
        return monoCluster.flatMap(cluster -> cluster.spreadGossip(message));
    }

    @Override
    public void onMembershipEvent(MembershipEvent event) {
        RSocketBroker broker = memberToBroker(event.member());
        String brokerIp = event.member().address().host();
        if (event.isAdded()) {
            makeJsonRpcCall(event.member(), "BrokerService.getConfiguration", null).subscribe(response -> {
                brokers.put(broker.getIp(), broker);
                Map<String, String> brokerConfiguration = response.getResult();
                if (brokerConfiguration != null && !brokerConfiguration.isEmpty()) {
                    String externalDomain = brokerConfiguration.get("rsocket.broker.externalDomain");
                    broker.setExternalDomain(externalDomain);
                }
                log.info(RsocketErrorCode.message("RST-300001", broker.getIp(), "added"));
            });
        } else if (event.isRemoved()) {
            brokers.remove(brokerIp);
            log.info(RsocketErrorCode.message("RST-300001", broker.getIp(), "removed"));
        } else if (event.isLeaving()) {
            RSocketBroker leavingBroker = brokers.remove(brokerIp);
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
        this.monoCluster.subscribe(Cluster::shutdown);
    }

    @Override
    public void destroy() throws Exception {
        this.stopLocalBroker();
    }
}
