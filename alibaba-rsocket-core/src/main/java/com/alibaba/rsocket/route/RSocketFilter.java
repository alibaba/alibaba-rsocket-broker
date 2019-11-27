package com.alibaba.rsocket.route;

import com.alibaba.rsocket.RSocketExchange;
import reactor.core.publisher.Mono;

/**
 * rsocket filter
 *
 * @author leijuan
 */
public interface RSocketFilter {
    /**
     * filter or not
     *
     * @param exchange request exchange
     * @return filter required or not
     */
    Mono<Boolean> shouldFilter(RSocketExchange exchange);

    /**
     * run filter logic, no block code
     *
     * @param exchange request exchange
     */
    Mono<Void> run(RSocketExchange exchange);
}
