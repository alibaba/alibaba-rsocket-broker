package com.alibaba.rsocket;

import io.rsocket.RSocket;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

/**
 * Abstract RSocket
 *
 * @author leijuan
 */
public class AbstractRSocket implements RSocket {

    private final MonoProcessor<Void> onClose = MonoProcessor.create();

    @Override
    public void dispose() {
        onClose.onComplete();
    }

    @Override
    public boolean isDisposed() {
        return onClose.isDisposed();
    }

    @Override
    public @NotNull Mono<Void> onClose() {
        return onClose;
    }

}
