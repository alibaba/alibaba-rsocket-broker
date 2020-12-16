package com.alibaba.spring.boot.rsocket.broker.services;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;
import com.alibaba.rsocket.events.ConfigEvent;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.spring.boot.rsocket.broker.BrokerAppContext;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.security.AuthenticationService;
import com.alibaba.spring.boot.rsocket.broker.security.JwtPrincipal;
import com.alibaba.spring.boot.rsocket.broker.security.RSocketAppPrincipal;
import io.rsocket.exceptions.InvalidException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

/**
 * config controller
 *
 * @author leijuan
 */
@RestController
@RequestMapping("/config")
public class ConfigController {
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private RSocketBrokerHandlerRegistry handlerRegistry;
    @Value("${rsocket.broker.auth-required:true}")
    private boolean authRequired;

    @PostMapping("/refresh/{appName}")
    public Mono<String> refresh(@PathVariable(name = "appName") String appName,
                                @RequestParam(name = "ip", required = false) String ip,
                                @RequestParam(name = "id", required = false) String id,
                                @RequestHeader(name = HttpHeaders.AUTHORIZATION) String jwtToken,
                                @RequestBody String body) {
        RSocketAppPrincipal appPrincipal = parseAppPrincipal(jwtToken);
        if (appPrincipal != null && appPrincipal.getSubject().equalsIgnoreCase("rsocket-admin")) {
            //update config for ip or id
            if (ip != null || id != null) {
                CloudEventImpl<ConfigEvent> configEvent = RSocketCloudEventBuilder.builder(new ConfigEvent(appName, "text/x-java-properties", body))
                        .withSource(BrokerAppContext.identity())
                        .build();
                return Flux.fromIterable(handlerRegistry.findByAppName(appName)).filter(handler -> {
                    AppMetadata appMetadata = handler.getAppMetadata();
                    return appMetadata.getUuid().equals(id) || appMetadata.getIp().equals(ip);
                }).flatMap(handler -> handler.fireCloudEventToPeer(configEvent)).then(Mono.just("success"));
            } else {
                return configurationService.put(appName + ":application.properties", body).map(aVoid -> "success");
            }
        } else {
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-500403")));
        }
    }

    @GetMapping("/last/{appName}")
    public Mono<String> fetch(@PathVariable(name = "appName") String appName, @RequestHeader(name = HttpHeaders.AUTHORIZATION) String jwtToken) {
        RSocketAppPrincipal appPrincipal = parseAppPrincipal(jwtToken);
        if (appPrincipal != null && (appName.equalsIgnoreCase(appPrincipal.getSubject()) || appPrincipal.getSubject().equalsIgnoreCase("rsocket-admin"))) {
            return configurationService.get(appName + ":application.properties");
        } else {
            return Mono.error(new InvalidException(RsocketErrorCode.message("RST-500403")));
        }
    }

    private RSocketAppPrincipal parseAppPrincipal(String jwtToken) {
        if (authRequired) {
            return authenticationService.auth("Bearer", jwtToken.substring(jwtToken.indexOf(" ") + 1));
        } else {
            return new JwtPrincipal(UUID.randomUUID().toString(), "rsocket-admin",
                    Collections.singletonList("mock_owner"),
                    new HashSet<>(Collections.singletonList("admin")),
                    Collections.emptySet(),
                    new HashSet<>(Collections.singletonList("default")),
                    new HashSet<>(Collections.singletonList("1")));
        }
    }
}
