package com.alibaba.rsocket;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Mutable context Test
 *
 * @author leijuan
 */
public class MutableContextTest {

    @Test
    public void testMonoContext() {
        MutableContext mutableContext = new MutableContext();
        mutableContext.put("welcome", "Hello ");
        Mono<String> mono = Mono.just("demo")
                .flatMap(text -> Mono.subscriberContext()
                        .map(ctx -> ctx.get("welcome.cn") + text))
                .subscriberContext(context -> context.put("welcome.cn", "你好"))
                .subscriberContext(mutableContext::putAll);
        StepVerifier.create(mono).expectNext("你好demo").verifyComplete();
    }
}
