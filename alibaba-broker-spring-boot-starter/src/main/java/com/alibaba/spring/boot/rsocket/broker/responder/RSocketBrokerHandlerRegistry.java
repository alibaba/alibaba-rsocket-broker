package com.alibaba.spring.boot.rsocket.broker.responder;

import com.alibaba.rsocket.listen.RSocketResponderHandlerFactory;
import io.cloudevents.v1.CloudEventImpl;
import org.eclipse.collections.api.multimap.Multimap;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * rsocket broker handler registry to create, manage all RSocket handler
 *
 * @author leijuan
 */
public interface RSocketBrokerHandlerRegistry extends RSocketResponderHandlerFactory {

    Collection<String> findAllAppNames();

    Collection<RSocketBrokerResponderHandler> findAll();

    Collection<RSocketBrokerResponderHandler> findByAppName(String appName);

    @Nullable
    RSocketBrokerResponderHandler findByUUID(String id);

    @Nullable
    RSocketBrokerResponderHandler findById(Integer id);

    void onHandlerRegistered(RSocketBrokerResponderHandler responderHandler);

    void onHandlerDisposed(RSocketBrokerResponderHandler responderHandler);

    Multimap<String, RSocketBrokerResponderHandler> appHandlers();

    Mono<Void> broadcast(String appName, final CloudEventImpl cloudEvent);
}
