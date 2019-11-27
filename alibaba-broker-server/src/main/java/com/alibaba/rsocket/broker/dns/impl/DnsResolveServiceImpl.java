package com.alibaba.rsocket.broker.dns.impl;

import com.alibaba.rsocket.broker.dns.Answer;
import com.alibaba.rsocket.broker.dns.DnsResolveService;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import io.netty.handler.codec.dns.DnsRecordType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DNS resolver service implementation
 *
 * @author leijuan
 */
@Service
public class DnsResolveServiceImpl implements DnsResolveService {
    @Autowired
    private RSocketBrokerHandlerRegistry handlerRegistry;
    private Multimap<String, String> domainToTypes = MultimapBuilder.treeKeys().hashSetValues().build();
    private Multimap<String, Answer> dnsRecordStore = MultimapBuilder.treeKeys().hashSetValues().build();

    public DnsResolveServiceImpl() {
        addRecords("www.taobao.com", DnsRecordType.A.name(), "47.246.24.234", "47.246.25.233");
    }

    @Override
    public Flux<Answer> resolve(String name, String type) {
        String key = name + ":" + type;
        DnsRecordType dnsRecordType = DnsRecordType.valueOf(type);
        if (dnsRecordStore.containsKey(key)) {
            return Flux.fromIterable(dnsRecordStore.get(key));
        }
        Collection<RSocketBrokerResponderHandler> handlers = handlerRegistry.findByAppName(name);
        if (handlers != null && !handlers.isEmpty()) {
            return Flux.fromIterable(handlers)
                    .filter(handler -> handler.getAppStatus().equals(AppStatusEvent.STATUS_SERVING))
                    .map(handler -> new Answer(handler.getAppMetadata().getName(), dnsRecordType.intValue(), 300, handler.getAppMetadata().getIp()));
        }
        return Flux.empty();
    }

    @Override
    public void addRecords(String name, String type, Collection<Answer> answers) {
        domainToTypes.put(name, type);
        dnsRecordStore.putAll(name + ":" + type, answers);
    }

    @Override
    public void addRecords(String name, String type, String... datas) {
        final DnsRecordType recordType = DnsRecordType.valueOf(type);
        addRecords(name, type,
                Stream.of(datas)
                        .map(data -> new Answer(name, recordType.intValue(), 300, data))
                        .collect(Collectors.toList()));

    }

    @Override
    public Flux<String> allDomains() {
        Set<String> names = new HashSet<>();
        names.addAll(handlerRegistry.findAllAppNames());
        names.addAll(domainToTypes.keySet());
        return Flux.fromIterable(names);
    }

}
