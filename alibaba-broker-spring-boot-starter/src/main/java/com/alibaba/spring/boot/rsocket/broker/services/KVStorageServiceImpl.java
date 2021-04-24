package com.alibaba.spring.boot.rsocket.broker.services;

import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.spring.boot.rsocket.broker.supporting.RSocketLocalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * configuration service implementation in memory
 *
 * @author leijuan
 */
@RSocketLocalService(serviceInterface = ConfigurationService.class)
@ConditionalOnMissingBean(ConfigurationService.class)
public class KVStorageServiceImpl implements ConfigurationService {
    private static Logger log = LoggerFactory.getLogger(KVStorageServiceImpl.class);
    private Set<String> appNames = new HashSet<>();
    private Map<String, String> snapshotStore = new ConcurrentHashMap<>();
    private Map<String, Sinks.Many<String>> watchNotification = new ConcurrentHashMap<>();

    public KVStorageServiceImpl() {
        //add test data for unit test
        appNames.add("rsocket-config-client");
        appNames.add("rsocket-user-client");
        appNames.add("rsocket-user-service");
        snapshotStore.put("rsocket-config-client:application.properties", "developer=leijuan");
        log.info(RsocketErrorCode.message("RST-302200", "Memory"));
    }

    @Override
    public Flux<String> getGroups() {
        return Flux.fromIterable(appNames).sort();
    }

    @Override
    public Flux<String> findNamesByGroup(String groupName) {
        return Flux.fromIterable(snapshotStore.keySet())
                .filter(name -> name.startsWith(groupName + ":"));
    }

    @Override
    public Mono<Void> put(String key, String value) {
        return Mono.fromRunnable(() -> {
            snapshotStore.put(key, value);
            if (key.contains(":")) {
                appNames.add(key.substring(0, key.indexOf(":")));
            }
            if (!watchNotification.containsKey(key)) {
                initNotification(key);
            }
            watchNotification.get(key).tryEmitNext(value);
        });
    }

    @Override
    public Mono<Void> delete(String key) {
        return Mono.fromRunnable(() -> {
            snapshotStore.remove(key);
            if (watchNotification.containsKey(key)) {
                watchNotification.get(key).tryEmitNext("");
            }
        });
    }

    @Override
    public Mono<String> get(String key) {
        if (snapshotStore.containsKey(key)) {
            return Mono.just(snapshotStore.get(key));
        }
        return Mono.empty();
    }

    @Override
    public Flux<String> watch(String key) {
        if (!watchNotification.containsKey(key)) {
            initNotification(key);
        }
        return Flux.create(sink -> watchNotification.get(key).asFlux().subscribe(sink::next));
    }

    private void initNotification(String appName) {
        watchNotification.put(appName, Sinks.many().replay().latest());
    }
}
