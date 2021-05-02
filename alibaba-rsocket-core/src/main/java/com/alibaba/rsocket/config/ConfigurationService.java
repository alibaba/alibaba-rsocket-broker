package com.alibaba.rsocket.config;

import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

/**
 * configuration service
 *
 * @author leijuan
 */
public interface ConfigurationService {
    Mono<String> get(@NotNull String appName, @NotNull String key);
}
