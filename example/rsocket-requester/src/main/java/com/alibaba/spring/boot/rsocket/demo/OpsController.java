package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import com.alibaba.rsocket.cloudevents.RSocketCloudEventBuilder;
import com.alibaba.rsocket.upstream.UpstreamClusterChangedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.extra.processor.TopicProcessor;

import java.util.Arrays;

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
        final CloudEventImpl<UpstreamClusterChangedEvent> cloudEvent = RSocketCloudEventBuilder
                .builder(upstreamClusterChangedEvent)
                .build();
        eventProcessor.onNext(cloudEvent);
        return "success";
    }
}
