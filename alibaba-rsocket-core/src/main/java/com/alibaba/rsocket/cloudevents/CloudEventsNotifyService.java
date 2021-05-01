package com.alibaba.rsocket.cloudevents;

import reactor.core.publisher.Mono;

/**
 * CloudEvents Notify service
 *
 * @author leijuan
 */
public interface CloudEventsNotifyService {

    public Mono<Void> notify(String appId, String cloudEventJson);

    public Mono<Void> notifyAll(String appName, String cloudEventJson);
}
