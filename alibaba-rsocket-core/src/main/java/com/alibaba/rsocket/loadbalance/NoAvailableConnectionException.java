package com.alibaba.rsocket.loadbalance;

/**
 * No available connection exception
 *
 * @author leijuan
 */
public class NoAvailableConnectionException extends Exception {
    public NoAvailableConnectionException(String message) {
        super(message);
    }
}
