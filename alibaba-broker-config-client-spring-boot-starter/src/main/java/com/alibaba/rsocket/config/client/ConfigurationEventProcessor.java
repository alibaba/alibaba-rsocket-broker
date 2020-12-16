package com.alibaba.rsocket.config.client;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.config.bootstrap.RSocketConfigPropertySourceLocator;
import com.alibaba.rsocket.events.CloudEventSupport;
import com.alibaba.rsocket.events.ConfigEvent;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.stereotype.Component;
import reactor.extra.processor.TopicProcessor;

import javax.annotation.PostConstruct;
import java.io.StringReader;
import java.util.Properties;

/**
 * Config Event Processor
 *
 * @author leijuan
 */
@SuppressWarnings("rawtypes")
@Component
public class ConfigurationEventProcessor {
    private Logger log = LoggerFactory.getLogger(ConfigurationEventProcessor.class);
    private ContextRefresher contextRefresher;
    private String applicationName;
    private TopicProcessor<CloudEventImpl> eventProcessor;

    public ConfigurationEventProcessor(TopicProcessor<CloudEventImpl> eventProcessor, ContextRefresher contextRefresher, String applicationName) {
        this.eventProcessor = eventProcessor;
        this.contextRefresher = contextRefresher;
        this.applicationName = applicationName;
    }

    @PostConstruct
    public void init() {
        eventProcessor.subscribe(cloudEvent -> {
            String type = cloudEvent.getAttributes().getType();
            if (ConfigEvent.class.getCanonicalName().equalsIgnoreCase(type)) {
                handleConfigurationEvent(cloudEvent);
            }
        });
    }

    public void handleConfigurationEvent(CloudEventImpl<?> cloudEvent) {
        // replyto support
        // cloudEvent.getExtensions().get("replyto"); rsocket:///REQUEST_FNF/com.xxxx.XxxService#method
        ConfigEvent configEvent = CloudEventSupport.unwrapData(cloudEvent, ConfigEvent.class);
        // validate config content
        if (configEvent!=null && applicationName.equalsIgnoreCase(configEvent.getAppName())
                && !RSocketConfigPropertySourceLocator.getLastConfigText().equals(configEvent.getContent())) {
            Properties configProperties = RSocketConfigPropertySourceLocator.CONFIG_PROPERTIES.get(applicationName);
            if (configProperties != null) {
                try {
                    configProperties.load(new StringReader(configEvent.getContent()));
                    log.info(RsocketErrorCode.message("RST-202200", applicationName));
                    contextRefresher.refresh();
                    RSocketConfigPropertySourceLocator.setLastConfigText(configEvent.getContent());
                    log.info(RsocketErrorCode.message("RST-202001"));
                } catch (Exception e) {
                    log.info(RsocketErrorCode.message("RST-202501"), e);
                }
            }
        }
    }
}
