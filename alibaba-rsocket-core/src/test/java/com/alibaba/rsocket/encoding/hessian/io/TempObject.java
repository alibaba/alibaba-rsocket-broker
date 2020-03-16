package com.alibaba.rsocket.encoding.hessian.io;

import java.io.Serializable;

/**
 * temp object
 *
 * @author linux_china
 */
public class TempObject implements Serializable {
    private Integer id;
    private String name;

    public TempObject() {
    }

    public TempObject(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
