package com.alibaba.rsocket.upstream;

import com.alibaba.rsocket.events.CloudEventSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Upstream Cluster changed event
 *
 * @author leijuan
 */
public class UpstreamClusterChangedEvent implements CloudEventSupport<UpstreamClusterChangedEvent> {
    private String interfaceName;
    private String group;
    private String version;
    private List<String> uris;
    private String defaultUri;

    public UpstreamClusterChangedEvent() {
    }

    public UpstreamClusterChangedEvent(String group, String interfaceName, String version, List<String> uris) {
        this.interfaceName = interfaceName;
        this.group = group;
        this.version = version;
        this.uris = uris;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public String getDefaultUri() {
        return defaultUri;
    }

    public void setDefaultUri(String defaultUri) {
        this.defaultUri = defaultUri;
    }

    @JsonIgnore
    public boolean isBrokerCluster() {
        return "*".equals(interfaceName);
    }
}
