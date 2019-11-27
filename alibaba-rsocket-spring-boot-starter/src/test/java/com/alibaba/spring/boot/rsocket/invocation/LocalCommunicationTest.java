package com.alibaba.spring.boot.rsocket.invocation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Flux;

/**
 * Local communication test
 *
 * @author leijuan
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LocalCommunicationTest extends LocalReactiveServiceTestCase {
    @Autowired
    @Qualifier("reactiveTestService")
    private ReactiveTestService reactiveTestService;

    @Test
    public void testRequestResponse() throws Exception {
        System.out.println(reactiveTestService.findNickById(1).block());
        System.out.println(reactiveTestService.findNickById(null).block());
    }

    @Test
    public void testRequestStream() {
        System.out.println(reactiveTestService.findNames().blockLast());
    }

    @Test
    public void testRequestChannel1() throws Exception {
        reactiveTestService.requestGlobals(Flux.just("action1", "action2")).subscribe(System.out::println);
        Thread.sleep(1000);
    }

    @Test
    public void testRequestChannel2() throws Exception {
        reactiveTestService.requestRecommendations("demo", Flux.just("action1", "action2")).subscribe(System.out::println);
        Thread.sleep(2000);
        reactiveTestService.requestRecommendations("demo", Flux.just("action1", "action2")).subscribe(System.out::println);
        Thread.sleep(2000);
    }
}
