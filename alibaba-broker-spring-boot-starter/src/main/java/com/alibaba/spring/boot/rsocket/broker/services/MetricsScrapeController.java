package com.alibaba.spring.boot.rsocket.broker.services;

import com.alibaba.rsocket.metadata.*;
import com.alibaba.rsocket.observability.MetricsService;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBroker;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Autowired
    private Environment env;
    @Autowired
    private RSocketBrokerManager brokerManager;

    public MetricsScrapeController() {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(
                new GSVRoutingMetadata(null, MetricsService.class.getCanonicalName(), "scrape", null),
                new MessageMimeTypeMetadata(RSocketMimeType.Text),
                new MessageAcceptMimeTypesMetadata(RSocketMimeType.Text));
        ByteBuf compositeMetadataContent = compositeMetadata.getContent();
        this.metricsScrapeCompositeByteBuf = Unpooled.copiedBuffer(compositeMetadataContent);
        ReferenceCountUtil.safeRelease(compositeMetadataContent);
    }

    @GetMapping("/prometheus/app/targets")
    public Mono<List<PrometheusAppInstanceConfig>> appTargets() {
        String port = env.getProperty("server.port");
        List<String> hosts = brokerManager.currentBrokers().stream().map(RSocketBroker::getIp).collect(Collectors.toList());
        int hostSize = hosts.size();
        return Mono.just(handlerRegistry.findAll().stream()
                .map(handler -> {
                    String host = hosts.get(handler.getId() % hostSize);
                    return new PrometheusAppInstanceConfig(host, port, "/metrics/" + handler.getUuid());
                })
                .collect(Collectors.toList()));
    }

    @GetMapping("/prometheus/broker/targets")
    public Mono<List<PrometheusAppInstanceConfig>> brokerTargets() {
        String port = env.getProperty("management.server.port");
        return Mono.just(brokerManager.currentBrokers().stream()
                .map(broker -> new PrometheusAppInstanceConfig(broker.getIp(), port, "/actuator/prometheus"))
                .collect(Collectors.toList()));
    }

    @GetMapping(value = "/{uuid}", produces = MimeTypeUtils.TEXT_PLAIN_VALUE)
    public Mono<String> scrape(@PathVariable(name = "uuid") String uuid) {
        RSocketBrokerResponderHandler rsocket = handlerRegistry.findByUUID(uuid);
        if (rsocket == null) {
            return Mono.error(new Exception(RsocketErrorCode.message("RST-300205", uuid)));
        }
        return rsocket.getPeerRsocket().requestResponse(ByteBufPayload.create(Unpooled.EMPTY_BUFFER, metricsScrapeCompositeByteBuf.retainedDuplicate()))
                .map(Payload::getDataUtf8);
    }

    public static class PrometheusAppInstanceConfig {
        private Map<String, String> labels = new HashMap<>();
        private List<String> targets = new ArrayList<>();

        public PrometheusAppInstanceConfig() {

        }

        public PrometheusAppInstanceConfig(String host, String port, String metricsPath) {
            targets.add(host + ":" + port);
            this.labels.put("__metrics_path__", metricsPath);
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public void setLabels(Map<String, String> labels) {
            this.labels = labels;
        }

        public List<String> getTargets() {
            return targets;
        }

        public void setTargets(List<String> targets) {
            this.targets = targets;
        }
    }
}
