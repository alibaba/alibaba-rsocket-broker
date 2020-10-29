package com.alibaba.rsocket.events;

import java.util.Map;

/**
 * ports update event
 *
 * @author leijuan
 */
public class PortsUpdateEvent implements CloudEventSupport<PortsUpdateEvent> {

    private String appId;
    private Map<Integer, String> rsocketPorts;
    private int webPort;
    private int managementPort;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Map<Integer, String> getRsocketPorts() {
        return rsocketPorts;
    }

    public void setRsocketPorts(Map<Integer, String> rsocketPorts) {
        this.rsocketPorts = rsocketPorts;
    }

    public int getWebPort() {
        return webPort;
    }

    public void setWebPort(int webPort) {
        this.webPort = webPort;
    }

    public int getManagementPort() {
        return managementPort;
    }

    public void setManagementPort(int managementPort) {
        this.managementPort = managementPort;
    }
}
