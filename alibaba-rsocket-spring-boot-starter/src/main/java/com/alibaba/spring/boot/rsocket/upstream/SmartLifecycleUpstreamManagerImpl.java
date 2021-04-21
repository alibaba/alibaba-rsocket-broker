package com.alibaba.spring.boot.rsocket.upstream;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;
import com.alibaba.rsocket.events.AppStatusEvent;
import com.alibaba.rsocket.upstream.UpstreamManagerImpl;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.context.SmartLifecycle;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * SmartLifecycle UpstreamManager implementation with graceful shutdown support
 *
 * @author leijuan
 */
public class SmartLifecycleUpstreamManagerImpl extends UpstreamManagerImpl implements SmartLifecycle {
    private int status = 0;
    @Autowired
    private ServerProperties serverProperties;

    public SmartLifecycleUpstreamManagerImpl(RSocketRequesterSupport rsocketRequesterSupport) {
        super(rsocketRequesterSupport);
    }

    @Override
    public void init() throws Exception {
        if (this.status == 0) {
            super.init();
            this.status = 1;
        }
    }

    @Override
    public void start() {
        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.status = 1;
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Stop must not be invoked directly");
    }

    @Override
    public void stop(final @NotNull Runnable callback) {
        this.status = -1;
        shutDownGracefully((result) -> callback.run());
    }

    @Override
    public boolean isRunning() {
        return status == 1;
    }

    void shutDownGracefully(GracefulShutdownCallback callback) {
        try {
            this.close();
            if (serverProperties.getShutdown() == Shutdown.GRACEFUL) {
                if (!requesterSupport().exposedServices().get().isEmpty()) {
                    notifyShutdown().subscribe();
                    // waiting for 15 seconds to broadcast shutdown message for service provider
                    Thread.sleep(15000);
                } else {
                    // waiting for 5 second to receive messages from upstream clusters
                    Thread.sleep(5000);
                }
            }
        } catch (Exception ignore) {

        } finally {
            callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
        }
    }

    private Mono<Void> notifyShutdown() {
        final CloudEventImpl<AppStatusEvent> appStatusEventCloudEvent = RSocketCloudEventBuilder
                .builder(new AppStatusEvent(RSocketAppContext.ID, AppStatusEvent.STATUS_STOPPED))
                .build();
        return Flux.fromIterable(findAllClusters()).flatMap(upstreamCluster -> upstreamCluster.getLoadBalancedRSocket().fireCloudEventToUpstreamAll(appStatusEventCloudEvent)).then();
    }

}
