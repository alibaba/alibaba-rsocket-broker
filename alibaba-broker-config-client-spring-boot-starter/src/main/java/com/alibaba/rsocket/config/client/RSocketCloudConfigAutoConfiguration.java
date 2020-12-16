package com.alibaba.rsocket.config.client;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.extra.processor.TopicProcessor;

/**
 * RSocket cloud config auto configuration
 *
 * @author leijuan
 */
@Configuration
public class RSocketCloudConfigAutoConfiguration {
    @Value("${spring.application.name}")
    private String applicationName;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public ConfigurationEventProcessor rSocketConfigListener(@Autowired ContextRefresher contextRefresher,
                                                             @Autowired @Qualifier("reactiveCloudEventProcessor") TopicProcessor<CloudEventImpl> eventProcessor) {
        return new ConfigurationEventProcessor(eventProcessor, contextRefresher, applicationName);
    }
}
