package com.alibaba.rsocket.broker.config;

import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.spring.boot.rsocket.broker.services.ConfigurationService;
import com.alibaba.spring.boot.rsocket.broker.supporting.RSocketLocalService;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.annotation.PreDestroy;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration Service with H2 MVStore implementation
 *
 * @author leijuan
 */
@RSocketLocalService(serviceInterface = ConfigurationService.class)
@ConditionalOnExpression("'${rsocket.broker.config-store}'.startsWith('h2://')")
public class ConfigurationServiceMVStoreImpl implements ConfigurationService {
    private static Logger log = LoggerFactory.getLogger(ConfigurationServiceMVStoreImpl.class);
    private MVStore mvStore;
    private Map<String, Sinks.Many<String>> watchNotification = new ConcurrentHashMap<>();

    public ConfigurationServiceMVStoreImpl() {
        File rsocketRootDir = new File(System.getProperty("user.home"), ".rsocket");
        if (!rsocketRootDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            rsocketRootDir.mkdirs();
        }
        try {
            mvStore = MVStore.open(new File(rsocketRootDir, "appsConfig.db").getAbsolutePath());
        } catch (Exception e) {
            String storeFilePath = new File(System.getProperty("java.io.tmpdir", "/tmp"), "appsConfig.db").getAbsolutePath();
            mvStore = MVStore.open(storeFilePath);
            log.info("Configuration store file: " + storeFilePath);
        }
        log.info(RsocketErrorCode.message("RST-302200", "H2 MVStore"));
    }

    @PreDestroy
    public void close() {
        this.mvStore.close();
    }

    @Override
    public Flux<String> getGroups() {
        return Flux.fromIterable(mvStore.getMapNames()).sort();
    }

    @Override
    public Flux<String> findNamesByGroup(String groupName) {
        MVMap<String, String> appMap = mvStore.openMap(groupName);
        if (appMap != null && !appMap.isEmpty()) {
            return Flux.fromIterable(appMap.keySet()).map(keyName -> groupName + ":" + keyName);
        }
        return Flux.empty();
    }

    @Override
    public Mono<Void> put(String key, String value) {
        return Mono.fromRunnable(() -> {
            String[] parts = key.split(":", 2);
            if (parts.length == 2) {
                mvStore.openMap(parts[0]).put(parts[1], value);
                mvStore.commit();
                if (!watchNotification.containsKey(key)) {
                    initNotification(key);
                }
                watchNotification.get(key).tryEmitNext(value);
            }
        });
    }

    @Override
    public Mono<Void> delete(String key) {
        return Mono.fromRunnable(() -> {
            String[] parts = key.split(":", 2);
            if (parts.length == 2) {
                mvStore.openMap(parts[0]).remove(parts[1]);
                mvStore.commit();
                if (watchNotification.containsKey(key)) {
                    watchNotification.get(key).tryEmitNext("");
                }
            }

        });
    }

    @Override
    public Mono<String> get(String key) {
        String[] parts = key.split(":", 2);
        if (mvStore.hasMap(parts[0])) {
            MVMap<String, String> appMap = mvStore.openMap(parts[0]);
            if (appMap.containsKey(parts[1])) {
                return Mono.just(appMap.get(parts[1]));
            }
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
