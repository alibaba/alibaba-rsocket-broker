package com.alibaba.rsocket.events;

/**
 * RateLimiterConfig event
 *
 * @author leijuan
 */
public class RateLimiterConfigEvent {
    /**
     * period of limit refresh, and unit is milli second
     */
    private int limitRefreshPeriod;
    /**
     * permissions limit for refresh period
     */
    private int limitForPeriod;
    /**
     * default wait for permission duration, and unit is milli second
     */
    private int timeoutDuration;


    public int getLimitRefreshPeriod() {
        return limitRefreshPeriod;
    }

    public void setLimitRefreshPeriod(int limitRefreshPeriod) {
        this.limitRefreshPeriod = limitRefreshPeriod;
    }

    public int getLimitForPeriod() {
        return limitForPeriod;
    }

    public void setLimitForPeriod(int limitForPeriod) {
        this.limitForPeriod = limitForPeriod;
    }

    public int getTimeoutDuration() {
        return timeoutDuration;
    }

    public void setTimeoutDuration(int timeoutDuration) {
        this.timeoutDuration = timeoutDuration;
    }
}
