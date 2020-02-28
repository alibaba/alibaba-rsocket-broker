package com.alibaba.rsocket.gateway;

import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.upstream.UpstreamManager;
import io.netty.buffer.ByteBuf;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
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
                                                @RequestHeader(name = "Authorization", required = false, defaultValue = "") String authorization) {
        //todo authorization: JWT, SAML etc
        // please add auth code here
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

}
