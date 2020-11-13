package com.alibaba.rsocket.events;

import java.util.List;

/**
 * DNS record changed event. For example, Reactor Netty TCPClient DNS resolver, OkHTTP3 DNS Resolver
 *
 * @author leijuan
 */
public class DnsRecordChangedEvent implements CloudEventSupport<DnsRecordChangedEvent> {
    /**
     * cache keys
     */
    private String host;
    /**
     * type for changing: -1: removed, 1: added, 0: replaced
     */
    private Integer type;
    private List<String> ipList;

    public DnsRecordChangedEvent(String host, List<String> ipList) {
        this.host = host;
        this.ipList = ipList;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public List<String> getIpList() {
        return ipList;
    }

    public void setIpList(List<String> ipList) {
        this.ipList = ipList;
    }
}
