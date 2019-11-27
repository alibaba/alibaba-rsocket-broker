package com.alibaba.rsocket.broker.web.model;

/**
 * Pair with key & value
 *
 * @author leijuan
 */
public class Pair {
    String name;
    String value;

    public Pair() {
    }

    public Pair(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
