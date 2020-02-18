package com.alibaba.rsocket.broker.filters;

import com.alibaba.rsocket.RSocketExchange;
import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.route.RSocketFilter;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Canary filter for services(Example only)
 *
 * @author leijuan
 */
public class CanaryFilter implements RSocketFilter {
    private List<String> canaryServices = Arrays.asList("com.alibaba.Service1", "com.alibaba.Service2");
    private static String canaryVersion = "canary";
    private int index = 0;

    @Autowired
    private ServiceRoutingSelector routingSelector;

    @Override
    public Mono<Boolean> shouldFilter(RSocketExchange exchange) {
        GSVRoutingMetadata routingMetadata = exchange.getRoutingMetadata();
        if (canaryServices.contains(routingMetadata.getService())) {
            String canaryRouting = ServiceLocator.serviceId(routingMetadata.getGroup(), routingMetadata.getService(), canaryVersion);
            if (routingSelector.findHandler(ServiceLocator.serviceHashCode(canaryRouting)) != null) {
                return Mono.just(true);
            }
        }
        return Mono.just(false);
    }

    @Override
    public Mono<Void> run(RSocketExchange exchange) {
        if (index % 10 < 3) {
            GSVRoutingMetadata routingMetadata = exchange.getRoutingMetadata();
            routingMetadata.setVersion(canaryVersion);
        }
        index = (index + 1) % 10;
        return Mono.empty();
    }
}
