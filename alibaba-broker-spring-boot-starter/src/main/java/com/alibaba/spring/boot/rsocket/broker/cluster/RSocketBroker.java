package com.alibaba.spring.boot.rsocket.broker.cluster;

/**
 * RSocket Broker
 *
 * @author linux_china
 */
public class RSocketBroker {
    private String id;
    private String schema = "tcp";
    private String ip;
    private String externalDomain;
    private int port = 9999;
    private Integer status = 1;
    private long startedAt;

    public RSocketBroker() {
    }

    public RSocketBroker(String ip) {
        this.ip = ip;
    }

    public RSocketBroker(String ip, String externalDomain) {
        this.ip = ip;
        this.externalDomain = externalDomain;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getExternalDomain() {
        return externalDomain;
    }

    public void setExternalDomain(String externalDomain) {
        this.externalDomain = externalDomain;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public boolean isActive() {
        return status >= 1;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public String getUrl() {
        return schema + "://" + ip + ":" + port;
    }

    public String getAliasUrl() {
        if(externalDomain==null) {
            return null;
        }
        if (externalDomain.contains("://")) {
            return this.externalDomain;
        } else {
            return schema + "://" + externalDomain + ":" + port;
        }
    }
}
