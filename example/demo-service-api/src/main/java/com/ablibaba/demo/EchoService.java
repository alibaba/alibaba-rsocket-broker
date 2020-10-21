package com.ablibaba.demo;

import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

/**
 * @author hupeiD
 */
public interface EchoService {

    Mono<String> echo(@NonNull String s);
}
