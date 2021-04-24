package com.alibaba.rsocket.broker.config;

import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.spring.boot.rsocket.broker.RSocketBrokerProperties;
import com.alibaba.spring.boot.rsocket.broker.services.ConfigurationService;
import com.alibaba.spring.boot.rsocket.broker.supporting.RSocketLocalService;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration Service with Redis implementation
 *
 * @author leijuan
 */
@RSocketLocalService(serviceInterface = ConfigurationService.class)
@ConditionalOnExpression("'${rsocket.broker.configStore}'.startsWith('redis://')")
public class ConfigurationServiceRedisImpl implements ConfigurationService {
    private static Logger log = LoggerFactory.getLogger(ConfigurationServiceRedisImpl.class);
    private String prefix = "rs.config.";
    RedisClient redisClient;
    StatefulRedisConnection<String, String> redisConnection;
    RedisReactiveCommands<String, String> redisCommands;
    private Map<String, Sinks.Many<String>> watchNotification = new ConcurrentHashMap<>();

    public ConfigurationServiceRedisImpl(RSocketBrokerProperties properties) {
        redisClient = RedisClient.create(properties.getConfigStore());
        redisConnection = redisClient.connect();
        redisCommands = redisConnection.reactive();
        log.info(RsocketErrorCode.message("RST-302200", "Redis"));
    }

    @PreDestroy
    public void close() {
        this.redisConnection.close();
        this.redisClient.shutdown();
    }

    @Override
    public Flux<String> getGroups() {
        return redisCommands.keys(prefix + "*")
                .map(this::trimPrefix)
                .sort();
    }

    @Override
    public Flux<String> findNamesByGroup(String groupName) {
        return redisCommands.hkeys(groupName);
    }

    @Override
    public Mono<Void> put(String key, String value) {
        return splitGroupAndName(key).flatMap(parts -> {
            return redisCommands.hset(getKeyFullName(parts[0]), parts[1], value)
                    .doOnNext(aLong -> {
                        if (!watchNotification.containsKey(key)) {
                            initNotification(key);
                        }
                        watchNotification.get(key).tryEmitNext(value);
                    });
        }).then();
    }

    @Override
    public Mono<Void> delete(String key) {
        return splitGroupAndName(key).flatMap(parts -> {
            return redisCommands.hdel(getKeyFullName(parts[0]), parts[1])
                    .doOnNext(aLong -> {
                        if (watchNotification.containsKey(key)) {
                            watchNotification.get(key).tryEmitNext("");
                        }
                    });
        }).then();

    }

    @Override
    public Mono<String> get(String key) {
        return splitGroupAndName(key).flatMap(parts -> redisCommands.hget(getKeyFullName(parts[0]), parts[1]));
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

    private String getKeyFullName(String key) {
        return prefix + key;
    }

    private String trimPrefix(String key) {
        return key.substring(prefix.length() + 1);
    }

    Mono<String[]> splitGroupAndName(String key) {
        String[] parts = key.split(":", 2);
        if (parts.length == 2) {
            return Mono.just(parts);
        } else {
            return Mono.empty();
        }
    }

}
