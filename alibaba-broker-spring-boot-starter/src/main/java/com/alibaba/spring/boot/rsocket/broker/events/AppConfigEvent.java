package com.alibaba.spring.boot.rsocket.broker.events;

import com.alibaba.rsocket.events.CloudEventSupport;

/**
 * App config event
 *
 * @author leijuan
 */
public class AppConfigEvent implements CloudEventSupport<AppConfigEvent> {
    private String appName;
    private String key;
    private String vale;

    public AppConfigEvent() {
    }

    public AppConfigEvent(String appName, String key, String vale) {
        this.appName = appName;
        this.key = key;
        this.vale = vale;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVale() {
        return vale;
    }

    public void setVale(String vale) {
        this.vale = vale;
    }
}
