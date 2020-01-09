package com.alibaba.rsocket.events;

/**
 * Event Reply Handler
 *
 * @author leijuan
 */
@FunctionalInterface
public interface EventReplyHandler {

    void accept(EventReply reply);
}
