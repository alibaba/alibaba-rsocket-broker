package com.alibaba.rsocket.observability;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Tracing Service
 *
 * @author leijuan
 */
public interface TracingService {
    
    Mono<Void> sendSpans(List<byte[]> encodedSpans);
}
