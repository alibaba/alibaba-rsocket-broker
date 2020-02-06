package com.alibaba.rsocket.broker.dns;

/**
 * DNS question
 *
 * @author leijuan
 */
public class Question {
    /**
     * FQDN with trailing dot
     */
    private String name;
    /**
     * Standard DNS RR type
     */
    private Integer type;

    public Question() {
    }

    public Question(String name, Integer type) {
        setName(name);
        this.type = (type == null ? 1 : type);
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
}
