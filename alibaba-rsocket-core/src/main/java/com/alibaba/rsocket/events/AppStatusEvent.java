package com.alibaba.rsocket.events;

/**
 * App status event
 *
 * @author leijuan
 */
public class AppStatusEvent implements CloudEventSupport<AppStatusEvent> {
    public static Integer STATUS_CONNECTED = 0;
    public static Integer STATUS_SERVING = 1;
    public static Integer STATUS_OUT_OF_SERVICE = 2;
    public static Integer STATUS_STOPPED = -1;
    /**
     * App UUID
     */
    private String id;
    /**
     * app status: 0:connect, 1: serving, 2: not serving  -1: stopped
     */
    private Integer status;

    public AppStatusEvent() {
    }

    public AppStatusEvent(String id, Integer status) {
        this.id = id;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

}
