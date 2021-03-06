package com.alibaba.rsocket.events;

import com.alibaba.rsocket.cloudevents.CloudEventImpl;
import reactor.core.publisher.Flux;
import reactor.extra.processor.TopicProcessor;

import java.util.List;

/**
 * CloudEvents Processor
 *
 * @author leijuan
 */
@SuppressWarnings("rawtypes")
public class CloudEventsProcessor {
    private List<CloudEventsConsumer> consumers;

    private TopicProcessor<CloudEventImpl> eventProcessor;

    public CloudEventsProcessor(TopicProcessor<CloudEventImpl> eventProcessor, List<CloudEventsConsumer> consumers) {
        this.eventProcessor = eventProcessor;
        this.consumers = consumers;
    }

    public void init() {
        eventProcessor.subscribe(cloudEvent -> {
            Flux.fromIterable(consumers)
                    .filter(consumer -> consumer.shouldAccept(cloudEvent))
                    .flatMap(consumer -> consumer.accept(cloudEvent))
                    .subscribe();
        });
    }

    public void addConsumer(CloudEventsConsumer consumer) {
        this.consumers.add(consumer);
    }

}
