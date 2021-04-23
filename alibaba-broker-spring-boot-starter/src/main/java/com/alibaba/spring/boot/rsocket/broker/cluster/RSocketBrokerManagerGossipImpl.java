package com.alibaba.spring.boot.rsocket.broker.cluster;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.Json;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.route.RSocketFilter;
import com.alibaba.rsocket.transport.NetworkUtil;
import com.alibaba.spring.boot.rsocket.broker.RSocketBrokerProperties;
import com.alibaba.spring.boot.rsocket.broker.cluster.jsonrpc.JsonRpcRequest;
import com.alibaba.spring.boot.rsocket.broker.cluster.jsonrpc.JsonRpcResponse;
import com.alibaba.spring.boot.rsocket.broker.events.AppConfigEvent;
import com.alibaba.spring.boot.rsocket.broker.events.RSocketFilterEnableEvent;
import com.alibaba.spring.boot.rsocket.broker.services.ConfigurationService;
import io.micrometer.core.instrument.Metrics;
import io.scalecube.cluster.Cluster;
import io.scalecube.cluster.ClusterImpl;
import io.scalecube.cluster.ClusterMessageHandler;
import io.scalecube.cluster.Member;
import io.scalecube.cluster.membership.MembershipEvent;
import io.scalecube.cluster.transport.api.Message;
import io.scalecube.net.Address;
import io.scalecube.transport.netty.tcp.TcpTransportFactory;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RSocket Broker Manager Gossip implementation
 *
 * @author leijuan
 */
public class RSocketBrokerManagerGossipImpl implements RSocketBrokerManager, ClusterMessageHandler, SmartLifecycle {
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
    @Autowired
    private ServerProperties serverProperties;
    private int status = 0;

    private Mono<Cluster> monoCluster;
    private RSocketBroker localBroker;
    /**
     * rsocket brokers, key is ip address
     */
    private Map<String, RSocketBroker> brokers = new HashMap<>();
    /**
     * brokers changes emitter processor
     */
    private Sinks.Many<Collection<RSocketBroker>> brokersEmitterProcessor = Sinks.many().multicast().onBackpressureBuffer();
    private KetamaConsistentHash<String> consistentHash;

    @Override
    public Flux<Collection<RSocketBroker>> requestAll() {
        return brokersEmitterProcessor.asFlux();
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
    public String getName() {
        return "gossip";
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
                    .data(onJsonRpcCall(request))
                    .build();
            this.monoCluster.flatMap(cluster -> cluster.send(message.sender(), replyMessage)).subscribe();
        }
    }

    public JsonRpcResponse onJsonRpcCall(JsonRpcRequest request) {
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
            String javaClass = gossip.header("javaClass");
            if (javaClass != null) {
                try {
                    Class<?> resultClass = Class.forName(javaClass);
                    String jsonText = gossip.data();
                    onCloudEvent(Json.decodeValue(jsonText, resultClass));
                } catch (Exception ignore) {

                }
            }
        }
    }

    public void onCloudEvent(CloudEventImpl<?> cloudEvent) {
        String type = cloudEvent.getAttributes().getType();
        Optional<?> cloudEventData = cloudEvent.getData();
        cloudEventData.ifPresent(data -> {
            if (RSocketFilterEnableEvent.class.getCanonicalName().equals(type)) {
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
                .header("javaClass", cloudEvent.getAttributes().getType())
                .data(Json.serializeAsText(cloudEvent))
                .build();
        return monoCluster.flatMap(cluster -> cluster.spreadGossip(message));
    }

    @Override
    public void onMembershipEvent(MembershipEvent event) {
        RSocketBroker broker = memberToBroker(event.member());
        String brokerIp = broker.getIp();
        if (event.isAdded()) {
            makeJsonRpcCall(event.member(), "BrokerService.getConfiguration", null).subscribe(response -> {
                brokers.put(brokerIp, broker);
                this.consistentHash.add(brokerIp);
                Map<String, String> brokerConfiguration = response.getResult();
                if (brokerConfiguration != null && !brokerConfiguration.isEmpty()) {
                    String externalDomain = brokerConfiguration.get("rsocket.broker.externalDomain");
                    broker.setExternalDomain(externalDomain);
                }
                log.info(RsocketErrorCode.message("RST-300001", broker.getIp(), "added"));
            });
        } else if (event.isRemoved()) {
            brokers.remove(brokerIp);
            this.consistentHash.remove(brokerIp);
            log.info(RsocketErrorCode.message("RST-300001", broker.getIp(), "removed"));
        } else if (event.isLeaving()) {
            brokers.remove(brokerIp);
            this.consistentHash.remove(brokerIp);
            log.info(RsocketErrorCode.message("RST-300001", broker.getIp(), "left"));
        }
        brokersEmitterProcessor.tryEmitNext(brokers.values());
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
    public RSocketBroker findConsistentBroker(String clientId) {
        String brokerIp = this.consistentHash.get(clientId);
        return this.brokers.get(brokerIp);
    }

    @Override
    public void start() {
        final String localIp = NetworkUtil.LOCAL_IP;
        monoCluster = new ClusterImpl()
                .config(clusterConfig -> clusterConfig.externalHost(localIp).externalPort(gossipListenPort))
                .membership(membershipConfig -> membershipConfig.seedMembers(seedMembers()).syncInterval(5_000))
                .transportFactory(TcpTransportFactory::new)
                .transport(transportConfig -> transportConfig.port(gossipListenPort))
                .handler(cluster1 -> this)
                .start();
        //subscribe and start & join the cluster
        monoCluster.subscribe();
        this.localBroker = new RSocketBroker(localIp, brokerProperties.getExternalDomain());
        this.consistentHash = new KetamaConsistentHash<>(12, Collections.singletonList(localIp));
        brokers.put(localIp, localBroker);
        log.info(RsocketErrorCode.message("RST-300002"));
        Metrics.globalRegistry.gauge("cluster.broker.count", this, (DoubleFunction<RSocketBrokerManagerGossipImpl>) brokerManagerGossip -> brokerManagerGossip.brokers.size());
        this.status = 1;
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Stop must not be invoked directly");
    }

    @Override
    public void stop(final @NotNull Runnable callback) {
        this.status = -1;
        shutDownGracefully((result) -> callback.run());
    }

    @Override
    public boolean isRunning() {
        return status == 1;
    }

    void shutDownGracefully(GracefulShutdownCallback callback) {
        try {
            this.stopLocalBroker();
            if (serverProperties.getShutdown() == Shutdown.GRACEFUL) {
                // waiting for 15 seconds to broadcast shutdown message
                Thread.sleep(15000);
            }
        } catch (Exception ignore) {

        } finally {
            callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
        }
    }
}
