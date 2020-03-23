package com.alibaba.rsocket.route;

import com.alibaba.rsocket.RSocketExchange;
import reactor.core.publisher.Mono;

/**
 * rsocket filter
 *
 * @author leijuan
 */
public abstract class RSocketFilter {
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * filter or not
     *
     * @param exchange request exchange
     * @return filter required or not
     */
    public abstract Mono<Boolean> shouldFilter(RSocketExchange exchange);

    /**
     * run filter logic, no block code
     *
     * @param exchange request exchange
     * @return Mono void
     */
    public abstract Mono<Void> run(RSocketExchange exchange);

    /**
     * filter name or description
     *
     * @return filter name
     */
   public abstract String name();

    /**
     * refresh filter logic dynamically
     *
     * @param properties properties configuration
     */
    public void refresh(String properties) {

    }
}
