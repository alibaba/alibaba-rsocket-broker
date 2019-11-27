package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent;
import io.cloudevents.CloudEvent;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.extra.processor.TopicProcessor;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * Ops Controller
 *
 * @author linux_china
 */
@RestController
public class OpsController {
    @Autowired
    private TopicProcessor<CloudEventImpl> eventProcessor;

    @PostMapping("/upstream/update")
    public String updateUpstream(@RequestBody String uris) throws Exception {
        UpstreamClusterChangedEvent upstreamClusterChangedEvent = new UpstreamClusterChangedEvent();
        upstreamClusterChangedEvent.setGroup("");
        upstreamClusterChangedEvent.setInterfaceName("*");
        upstreamClusterChangedEvent.setVersion("");
        upstreamClusterChangedEvent.setUris(Arrays.asList(uris.split(",")));

        // passing in the given attributes
        final CloudEventImpl<UpstreamClusterChangedEvent> cloudEvent = CloudEventBuilder.<UpstreamClusterChangedEvent>builder()
                .withType("com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent")
                .withId(UUID.randomUUID().toString())
                .withTime(ZonedDateTime.now())
                .withDataschema(URI.create("rsocket:event"))
                .withDataContentType("application/json")
                .withSource(new URI("app://" + RSocketAppContext.ID))
                .withData(upstreamClusterChangedEvent)
                .build();
        eventProcessor.onNext(cloudEvent);
        return "success";
    }
}
