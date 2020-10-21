package com.example.demoresponder.service;

import com.ablibaba.demo.EchoService;
import com.alibaba.rsocket.RSocketService;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.logging.Level;

/**
 * @author hupeiD
 */
@RSocketService(serviceInterface = EchoService.class)
public class EchoServiceImpl implements EchoService {

    private static final Logger log = Loggers.getLogger(EchoServiceImpl.class);

    @Override
    public Mono<String> echo(@NotNull String s) {
        return Mono.just("Echo: " + s)
                   .log(log, Level.INFO, false, SignalType.ON_NEXT);
    }
}
