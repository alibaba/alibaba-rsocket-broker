package com.alibaba.user;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

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
    private Date createdAt;
    private LocalDateTime updatedAt;

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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
