package com.alibaba.rsocket.gateway;

import com.alibaba.rsocket.gateway.auth.JwtAuthenticationService;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.rsocket.upstream.UpstreamManager;
import io.netty.buffer.ByteBuf;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static io.netty.buffer.Unpooled.EMPTY_BUFFER;

/**
 * main controller
 *
 * @author leijuan
 */
@Controller
public class MainController {
    @Autowired
    private JwtAuthenticationService authenticationService;
    @Value("${restapi.auth-required}")
    private boolean authRequired;
    private static MessageMimeTypeMetadata jsonMetaEncoding = new MessageMimeTypeMetadata(RSocketMimeType.Json);
    private RSocket rsocket;

    public MainController(UpstreamManager upstreamManager) {
        rsocket = upstreamManager.findBroker().getLoadBalancedRSocket();
    }

    @RequestMapping(value = "/{serviceName}/{method}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<ResponseEntity<ByteBuf>> handle(@PathVariable("serviceName") String serviceName,
                                                @PathVariable("method") String method,
                                                @RequestParam(name = "group", required = false, defaultValue = "") String group,
                                                @RequestParam(name = "version", required = false, defaultValue = "") String version,
                                                @RequestBody(required = false) ByteBuf body,
                                                @RequestHeader(name = "Authorization", required = false, defaultValue = "") String authorizationValue) {
        boolean authenticated;
        if (!authRequired) {
            authenticated = true;
        } else {
            authenticated = authAuthorizationValue(authorizationValue);
        }
        if (!authenticated) {
            return Mono.error(new Exception(RsocketErrorCode.message("RST-500403")));
        }
        try {
            GSVRoutingMetadata routingMetadata = new GSVRoutingMetadata(group, serviceName, method, version);
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(routingMetadata, jsonMetaEncoding);
            ByteBuf bodyBuf = body == null ? EMPTY_BUFFER : body;
            return rsocket.requestResponse(ByteBufPayload.create(bodyBuf, compositeMetadata.getContent()))
                    .map(payload -> {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
                        return new ResponseEntity<>(payload.data(), headers, HttpStatus.OK);
                    });
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private boolean authAuthorizationValue(String authorizationValue) {
        if (authorizationValue == null || authorizationValue.isEmpty()) {
            return false;
        }
        String jwtToken = authorizationValue;
        if (authorizationValue.contains(" ")) {
            jwtToken = authorizationValue.substring(authorizationValue.lastIndexOf(" ") + 1);
        }
        return authenticationService.auth(jwtToken) != null;
    }

}
