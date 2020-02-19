package com.alibaba.spring.boot.rsocket.broker.cluster;

import com.alibaba.rsocket.ServiceLocator;
import io.cloudevents.v1.CloudEventImpl;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static com.alibaba.rsocket.transport.NetworkUtil.getLocalIP;

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
            String localIP = getLocalIP();
            this.localBroker = new RSocketBroker(localIP);
            this.hosts = Collections.singletonList(localIP);
        } catch (Exception ignore) {

        }
    }

    public DefaultRSocketBrokerManager(String... hosts) {
        this.localBroker = new RSocketBroker(getLocalIP());
        this.hosts = Arrays.asList(hosts);
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
    public Boolean isStandAlone() {
        return true;
    }

    @Override
    public void stopLocalBroker() {

    }

    @Override
    public Mono<String> spread(CloudEventImpl<?> cloudEvent) {
        return Mono.empty();
    }

    public Collection<RSocketBroker> hostsToBrokers() {
        return this.hosts.stream().map(host -> {
            RSocketBroker broker = new RSocketBroker();
            broker.setIp(host);
            return broker;
        }).collect(Collectors.toList());
    }


}
