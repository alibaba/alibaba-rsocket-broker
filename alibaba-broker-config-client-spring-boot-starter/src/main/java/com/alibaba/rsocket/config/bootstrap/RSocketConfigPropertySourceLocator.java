package com.alibaba.rsocket.config.bootstrap;

import com.alibaba.rsocket.observability.RsocketErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * RSocket Config properties source locator  with RSocket Broker
 *
 * @author leijuan
 */
public class RSocketConfigPropertySourceLocator implements PropertySourceLocator {
    private Logger log = LoggerFactory.getLogger(RSocketConfigPropertySourceLocator.class);
    /**
     * config properties. key is app name
     */
    public static final Map<String, Properties> CONFIG_PROPERTIES = new HashMap<>();
    private static final Map<String, PropertiesPropertySource> CONFIG_SOURCES = new HashMap<>();
    /**
     * last config text
     */
    private static String LAST_CONFIG_TEXT = null;

    @Override
    public PropertySource<?> locate(Environment environment) {
        String jwtToken = environment.getProperty("rsocket.jwt-token");
        String rsocketBrokers = environment.getProperty("rsocket.brokers");
        String applicationName = environment.getProperty("spring.application.name");
        if (CONFIG_SOURCES.containsKey(applicationName)) {
            return CONFIG_SOURCES.get(applicationName);
        }
        if (jwtToken != null && rsocketBrokers != null && applicationName != null) {
            Properties configProperties = new Properties();
            for (String rsocketBroker : rsocketBrokers.split(",")) {
                URI rsocketURI = URI.create(rsocketBroker);
                String httpUri = "http://" + rsocketURI.getHost() + ":" + (rsocketURI.getPort() - 1) + "/config/last/" + applicationName;
                try {
                    String configText = WebClient.create().get()
                            .uri(httpUri)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();
                    if (configText != null && !configText.isEmpty()) {
                        LAST_CONFIG_TEXT = configText;
                        configProperties.load(new StringReader(LAST_CONFIG_TEXT));
                        CONFIG_PROPERTIES.put(applicationName, configProperties);
                        log.info(RsocketErrorCode.message("RST-202200", applicationName));
                    } else {
                        log.info(RsocketErrorCode.message("RST-202404", applicationName));
                    }
                    configProperties.setProperty("rsocket.metadata.config", "true");
                    PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource("rsocket-broker", configProperties);
                    CONFIG_SOURCES.put(applicationName, propertiesPropertySource);
                    return propertiesPropertySource;
                } catch (Exception e) {
                    log.error(RsocketErrorCode.message("RST-202500", httpUri), e);
                }
            }
        }
        log.error(RsocketErrorCode.message("RST-202201"));
        throw new RuntimeException(RsocketErrorCode.message("RST-202201"));
    }

    public static String getLastConfigText() {
        return LAST_CONFIG_TEXT;
    }

    public static void setLastConfigText(String configText) {
        if (configText != null && !configText.isEmpty()) {
            LAST_CONFIG_TEXT = configText;
        }
    }
}
