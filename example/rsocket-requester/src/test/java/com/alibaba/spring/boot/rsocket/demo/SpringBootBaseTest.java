package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.rsocket.upstream.UpstreamManagerMock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Sinks;

@SpringBootTest
@Import(SpringBootBaseTest.RSocketTestConfig.class)
@TestPropertySource(properties = {"rsocket.disabled=true"})
public abstract class SpringBootBaseTest {

    @SuppressWarnings({"deprecation", "rawtypes"})
    @TestConfiguration
    static class RSocketTestConfig {

        @Bean
        public UpstreamManager upstreamManager() {
            return new UpstreamManagerMock();
        }

        @Bean
        public Sinks.Many<CloudEventImpl> reactiveCloudEventProcessor() {
            return Sinks.many().multicast().onBackpressureBuffer();
        }

    }
}
