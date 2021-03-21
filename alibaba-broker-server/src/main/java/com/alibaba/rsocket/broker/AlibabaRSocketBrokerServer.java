package com.alibaba.rsocket.broker;

import com.alibaba.rsocket.encoding.RSocketEncodingFacade;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

/**
 * Alibaba RSocket Broker Server
 *
 * @author leijuan
 */
@SpringBootApplication
@StyleSheet("styles/styles.css")
@Theme(themeClass = Lumo.class)
@Viewport("width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes, viewport-fit=cover")
@Push
public class AlibabaRSocketBrokerServer implements AppShellConfigurator {
    public static final LocalDateTime STARTED_AT = LocalDateTime.now();

    public static void main(String[] args) {
        //checking encoder first
        //noinspection ResultOfMethodCallIgnored
        RSocketEncodingFacade.getInstance();
        SpringApplication.run(AlibabaRSocketBrokerServer.class, args);
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "alibaba-rsocket-broker");
    }

    /*@Bean
    public RSocketListenerCustomizer websocketListenerCustomizer() {
        return builder -> {
            builder.listen("ws", 19999);
        };
    }*/
}
