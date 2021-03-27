package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.rsocket.upstream.UpstreamManagerMock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import reactor.extra.processor.TopicProcessor;

@SpringBootTest
@Import(SpringBootBaseTest.TestConfig.class)
@TestPropertySource(properties = {"rsocket.disabled=true"})
public abstract class SpringBootBaseTest {

    @SuppressWarnings({"deprecation", "rawtypes"})
    @TestConfiguration
    static class TestConfig {

        @Bean(initMethod = "init")
        public UpstreamManager upstreamManager() {
            return new UpstreamManagerMock();
        }

        @Bean
        public TopicProcessor<CloudEventImpl> reactiveCloudEventProcessor() {
            return TopicProcessor.<CloudEventImpl>builder().name("cloud-events-processor").build();
        }

    }
}
