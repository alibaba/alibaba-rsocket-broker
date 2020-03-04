package com.alibaba.rsocket.events;

/**
 * App status event
 *
 * @author leijuan
 */
public class AppStatusEvent implements CloudEventSupport<AppStatusEvent> {
    public static final Integer STATUS_CONNECTED = 0;
    public static final Integer STATUS_SERVING = 1;
    public static final Integer STATUS_OUT_OF_SERVICE = 2;
    public static final Integer STATUS_STOPPED = -1;
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

    public static String statusText(Integer status) {
        if (STATUS_CONNECTED.equals(status)) {
            return "Connected";
        } else if (STATUS_SERVING.equals(status)) {
            return "Serving";
        } else if (STATUS_OUT_OF_SERVICE.equals(status)) {
            return "OutOfService";
        } else if (STATUS_STOPPED.equals(status)) {
            return "Stopped";
        } else {
            return "Unknown";
        }
    }

}
