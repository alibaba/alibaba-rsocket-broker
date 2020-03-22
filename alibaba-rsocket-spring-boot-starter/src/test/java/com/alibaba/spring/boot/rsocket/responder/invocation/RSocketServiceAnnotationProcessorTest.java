package com.alibaba.spring.boot.rsocket.responder.invocation;

import com.alibaba.spring.boot.rsocket.RSocketProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * RSocket Service Annotation Processor test
 *
 * @author leijuan
 */
public class RSocketServiceAnnotationProcessorTest {
    private RSocketServiceAnnotationProcessor processor;

    @BeforeAll
    public void setUp() {
        processor = new RSocketServiceAnnotationProcessor(new RSocketProperties());
    }

    @Test
    public void testScanRSocketServiceAnnotation() {
        ReactiveTestService reactiveTestService = new ReactiveTestServiceImpl();
        processor.scanRSocketServiceAnnotation(reactiveTestService, "reactiveTestService");
        Assertions.assertTrue(processor.contains(ReactiveTestService.class.getCanonicalName()));
        Assertions.assertNotNull(processor.getInvokeMethod(ReactiveTestService.class.getCanonicalName(),"findNickById"));

    }
}
