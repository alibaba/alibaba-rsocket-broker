package com.alibaba.rsocket.observability;

import reactor.core.publisher.Mono;

/**
 * Metrics service
 *
 * @author leijuan
 */
public interface MetricsService {
    //com.alibaba.rsocket.observability.MetricsService
    public Mono<String> scrape();
}
