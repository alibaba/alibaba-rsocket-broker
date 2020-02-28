package com.alibaba.rsocket.broker.api;

import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceMeshInspector;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.alibaba.spring.boot.rsocket.broker.security.AuthenticationService;
import com.alibaba.spring.boot.rsocket.broker.security.RSocketAppPrincipal;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.util.DefaultPayload;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static io.netty.buffer.Unpooled.EMPTY_BUFFER;

/**
 * RSocket REST API controller
 *
 * @author leijuan
 */
@RestController
@RequestMapping("/api")
public class RSocketRestApiController {
    @Value("${rsocket.broker.auth-required}")
    private boolean authRequired;
    @Autowired
    private ServiceRoutingSelector routingSelector;
    @Autowired
    private RSocketBrokerHandlerRegistry handlerRegistry;
    @Autowired
    private ServiceMeshInspector serviceMeshInspector;
    @Autowired
    private AuthenticationService authenticationService;
    private static MessageMimeTypeMetadata jsonMetaEncoding = new MessageMimeTypeMetadata(RSocketMimeType.Json);


    @RequestMapping(value = "/{serviceName}/{method}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<ResponseEntity<String>> handle(@PathVariable("serviceName") String serviceName,
                                               @PathVariable("method") String method,
                                               @RequestParam(name = "group", required = false, defaultValue = "") String group,
                                               @RequestParam(name = "version", required = false, defaultValue = "") String version,
                                               @RequestBody(required = false) byte[] body,
                                               @RequestHeader(name = "X-Endpoint", required = false, defaultValue = "") String endpoint,
                                               @RequestHeader(name = "Authorization", required = false, defaultValue = "") String authorizationValue) {
        try {
            GSVRoutingMetadata routingMetadata = new GSVRoutingMetadata(group, serviceName, method, version);
            Integer serviceHashCode = routingMetadata.id();
            Integer targetHandlerId = routingSelector.findHandler(serviceHashCode);
            if (!endpoint.isEmpty() && endpoint.startsWith("id:")) {
                targetHandlerId = Integer.valueOf(endpoint.substring(3).trim());
            }
            if (targetHandlerId != null) {
                RSocketBrokerResponderHandler targetHandler = handlerRegistry.findById(targetHandlerId);
                if (targetHandler != null) {
                    if (authRequired) {
                        RSocketAppPrincipal principal = authAuthorizationValue(authorizationValue);
                        if (principal == null || !serviceMeshInspector.isRequestAllowed(principal, routingMetadata.gsv(), targetHandler.getPrincipal())) {
                            return Mono.just(error(RsocketErrorCode.message("RST-900401", routingMetadata.gsv())));
                        }
                    }
                    RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(routingMetadata, jsonMetaEncoding);
                    ByteBuf bodyBuf = body == null ? EMPTY_BUFFER : Unpooled.wrappedBuffer(body);
                    return targetHandler.requestResponse(DefaultPayload.create(bodyBuf, compositeMetadata.getContent()))
                            .map(payload -> {
                                HttpHeaders headers = new HttpHeaders();
                                headers.setContentType(MediaType.APPLICATION_JSON);
                                headers.setCacheControl(CacheControl.noCache().getHeaderValue());
                                return new ResponseEntity<>(payload.getDataUtf8(), headers, HttpStatus.OK);
                            });
                }
            }
            return Mono.just(error(RsocketErrorCode.message("RST-900404", routingMetadata.gsv())));
        } catch (Exception e) {
            return Mono.just(error(e.getMessage()));
        }
    }

    @Nullable
    private RSocketAppPrincipal authAuthorizationValue(String authorizationValue) {
        if (authorizationValue == null || authorizationValue.isEmpty()) {
            return null;
        }
        String jwtToken = authorizationValue;
        if (authorizationValue.contains(" ")) {
            jwtToken = authorizationValue.substring(authorizationValue.lastIndexOf(" ") + 1);
        }
        return authenticationService.auth("jwt", jwtToken);
    }

    public ResponseEntity<String> error(String errorText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        return new ResponseEntity<>(errorText, headers, HttpStatus.BAD_REQUEST);
    }

}
