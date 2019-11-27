package com.alibaba.rsocket.broker.dns;

import reactor.core.publisher.Flux;

import java.util.Collection;

/**
 * DNS Resolver Service
 *
 * @author leijuan
 */
public interface DnsResolveService {

    Flux<Answer> resolve(String name, String type);

    void addRecords(String name, String type, Collection<Answer> answers);

    void addRecords(String name, String type, String... datas);

    Flux<String> allDomains();
}
