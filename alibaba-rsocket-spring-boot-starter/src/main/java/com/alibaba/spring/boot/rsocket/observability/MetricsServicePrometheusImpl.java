package com.alibaba.spring.boot.rsocket.observability;

import com.alibaba.rsocket.RSocketService;
import com.alibaba.rsocket.observability.MetricsService;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import reactor.core.publisher.Mono;

/**
 * metrics service Prometheus implementation
 *
 * @author leijuan
 */
@RSocketService(serviceInterface = MetricsService.class)
public class MetricsServicePrometheusImpl implements MetricsService {
    private PrometheusMeterRegistry meterRegistry;

    public MetricsServicePrometheusImpl(PrometheusMeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<String> scrape() {
        return Mono.fromCallable(() -> meterRegistry.scrape());
    }
}
