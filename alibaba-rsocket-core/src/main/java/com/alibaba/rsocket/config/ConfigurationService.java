package com.alibaba.rsocket.config;

import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

/**
 * configuration service
 *
 * @author leijuan
 */
public interface ConfigurationService {
    /**
     * get app's configuration
     *
     * @param appName app name
     * @param key     key for configuration, such as application.properties
     * @return configuration's content
     */
    Mono<String> get(@NotNull String appName, @NotNull String key);
}
