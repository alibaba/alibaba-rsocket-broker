package com.alibaba.spring.boot.rsocket.broker.upstream;

import java.util.List;

/**
 * upstream cluster
 *
 * @author leijuan
 */
public class UpstreamCluster {
    private List<String> brokers;
    private String jwtToken;

    public List<String> getBrokers() {
        return brokers;
    }

    public void setBrokers(List<String> brokers) {
        this.brokers = brokers;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }
}
