package com.alibaba.user;

import java.io.Serializable;

/**
 * user class
 *
 * @author leijuan
 */
public class User implements Serializable {
    private Integer id;
    private String nick;
    private String email;
    private String phone;

    public User() {
    }

    public User(Integer id, String nick) {
        this.id = id;
        this.nick = nick;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
