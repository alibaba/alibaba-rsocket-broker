package com.alibaba.spring.boot.rsocket.broker.cluster;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.transport.NetworkUtil;
import io.micrometer.core.instrument.Metrics;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;


/**
 * Default RSocket Broker Manager
 *
 * @author leijuan
 */
public class DefaultRSocketBrokerManager implements RSocketBrokerManager {
    private Collection<String> hosts;
    private RSocketBroker localBroker;

    public DefaultRSocketBrokerManager() {
        try {
            String localIP = NetworkUtil.LOCAL_IP;
            this.localBroker = new RSocketBroker(localIP);
            this.hosts = Collections.singletonList(localIP);
        } catch (Exception ignore) {

        }
    }

    public DefaultRSocketBrokerManager(String... hosts) {
        this.localBroker = new RSocketBroker(NetworkUtil.LOCAL_IP);
        this.hosts = Arrays.asList(hosts);
        Metrics.globalRegistry.gauge("cluster.broker.count", this, (DoubleFunction<DefaultRSocketBrokerManager>) brokerManagerGossip -> brokerManagerGossip.hosts.size());
    }

    @Override
    public Flux<Collection<RSocketBroker>> requestAll() {
        return Flux.just(hostsToBrokers());
    }

    @Override
    public RSocketBroker localBroker() {
        return this.localBroker;
    }

    @Override
    public Collection<RSocketBroker> currentBrokers() {
        return hostsToBrokers();
    }

    @Override
    public Mono<RSocketBroker> findByIp(String ip) {
        return Mono.justOrEmpty(hostsToBrokers().stream().filter(rSocketBroker -> rSocketBroker.getIp().equals(ip)).findFirst());
    }

    @Override
    public Flux<ServiceLocator> findServices(String ip) {
        return null;
    }

    @Override
    public String getName() {
        return "standalone";
    }

    @Override
    public Boolean isStandAlone() {
        return true;
    }

    @Override
    public void stopLocalBroker() {

    }

    @Override
    public Mono<String> broadcast(CloudEventImpl<?> cloudEvent) {
        return Mono.empty();
    }

    public Collection<RSocketBroker> hostsToBrokers() {
        return this.hosts.stream().map(host -> {
            RSocketBroker broker = new RSocketBroker();
            broker.setIp(host);
            return broker;
        }).collect(Collectors.toList());
    }

    @Override
    public RSocketBroker findConsistentBroker(String clientId) {
        return this.localBroker;
    }
}
