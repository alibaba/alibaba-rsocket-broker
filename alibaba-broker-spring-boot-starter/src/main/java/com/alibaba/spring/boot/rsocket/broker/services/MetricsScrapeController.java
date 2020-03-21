package com.alibaba.spring.boot.rsocket.broker.services;

import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.MetricsService;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * metrics scrape controller: scrape metrics from apps
 *
 * @author leijuan
 */
@RestController
@RequestMapping("/metrics")
public class MetricsScrapeController {
    private ByteBuf metricsScrapeCompositeByteBuf;
    @Autowired
    private RSocketBrokerHandlerRegistry handlerRegistry;

    public MetricsScrapeController() {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(
                new GSVRoutingMetadata(null, MetricsService.class.getCanonicalName(), "scrape", null),
                new MessageMimeTypeMetadata(RSocketMimeType.Hessian));
        ByteBuf compositeMetadataContent = compositeMetadata.getContent();
        this.metricsScrapeCompositeByteBuf = Unpooled.copiedBuffer(compositeMetadataContent);
        ReferenceCountUtil.safeRelease(compositeMetadataContent);
    }

    @GetMapping("/{uuid}")
    public Mono<String> scrape(@PathVariable(name = "uuid") String uuid) {
        RSocketBrokerResponderHandler rsocket = handlerRegistry.findByUUID(uuid);
        if (rsocket == null) {
            return Mono.error(new Exception(RsocketErrorCode.message("RST-300205", uuid)));
        }
        return rsocket.getPeerRsocket().requestResponse(ByteBufPayload.create(Unpooled.EMPTY_BUFFER, metricsScrapeCompositeByteBuf.retainedDuplicate()))
                .map(Payload::getDataUtf8);
    }
}
