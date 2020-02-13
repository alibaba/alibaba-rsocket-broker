package com.alibaba.rsocket.observability;

import reactor.core.publisher.Mono;

/**
 * Metrics service for scrape
 *
 * @author leijuan
 */
public interface MetricsService {
    Mono<String> scrape();
}
