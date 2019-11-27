package com.alibaba.rsocket.broker.dns;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DNS Answer
 *
 * @author leijuan
 */
public class Answer {
    /**
     * Always matches name in the Question section
     */
    private String name;
    /**
     * Standard DNS RR type
     */
    private Integer type = 1;
    /**
     * Record's time-to-live in seconds, and default is 5 minutes
     */
    @JsonProperty("TTL")
    private int ttl = 300;
    /**
     * Data for A - IP address as text
     */
    private String data;

    public Answer(String name, Integer type, Integer ttl, String data) {
        setName(name);
        this.type = type;
        if (ttl != null && ttl != 0) {
            this.ttl = ttl;
        }
        this.data = data;
    }

    public Answer() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name.endsWith(".")) {
            this.name = name;
        } else {
            this.name = name + ".";
        }
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
