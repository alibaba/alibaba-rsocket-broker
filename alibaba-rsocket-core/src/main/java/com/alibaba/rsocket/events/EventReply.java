package com.alibaba.rsocket.events;

/**
 * Event Reply
 *
 * @author leijuan
 */
public class EventReply {
    /**
     * source id of event
     */
    private String eventId;
    /**
     * logical id of reply
     */
    private String replyId;
    /**
     * reply name
     */
    private String replyName;
    private Integer status;
    private String data;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getReplyId() {
        return replyId;
    }

    public void setReplyId(String replyId) {
        this.replyId = replyId;
    }

    public String getReplyName() {
        return replyName;
    }

    public void setReplyName(String replyName) {
        this.replyName = replyName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
