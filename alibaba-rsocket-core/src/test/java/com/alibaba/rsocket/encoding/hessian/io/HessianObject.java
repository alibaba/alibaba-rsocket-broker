package com.alibaba.rsocket.encoding.hessian.io;

import java.time.*;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Hessian object for serializer test
 *
 * @author leijuan
 */
public class HessianObject implements java.io.Serializable {
    private LocalDateTime localDateTime;
    private LocalDate localDate;
    private LocalTime localTime;
    private Instant instant;
    private Duration duration;
    private Period period;
    private MonthDay monthDay;
    private YearMonth yearMonth;
    private Optional<String> optional;
    private Locale locale;
    private OffsetTime offsetTime;
    private OffsetDateTime offsetDateTime;
    private ZonedDateTime zonedDateTime;
    private UUID uuid;
    private Long[] longArray;
    private String emojiBear = "我是🐻";

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public LocalTime getLocalTime() {
        return localTime;
    }

    public void setLocalTime(LocalTime localTime) {
        this.localTime = localTime;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public MonthDay getMonthDay() {
        return monthDay;
    }

    public void setMonthDay(MonthDay monthDay) {
        this.monthDay = monthDay;
    }

    public YearMonth getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(YearMonth yearMonth) {
        this.yearMonth = yearMonth;
    }

    public Optional<String> getOptional() {
        return optional;
    }

    public void setOptional(Optional<String> optional) {
        this.optional = optional;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public OffsetTime getOffsetTime() {
        return offsetTime;
    }

    public void setOffsetTime(OffsetTime offsetTime) {
        this.offsetTime = offsetTime;
    }

    public OffsetDateTime getOffsetDateTime() {
        return offsetDateTime;
    }

    public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
        this.offsetDateTime = offsetDateTime;
    }

    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    public void setZonedDateTime(ZonedDateTime zonedDateTime) {
        this.zonedDateTime = zonedDateTime;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Long[] getLongArray() {
        return longArray;
    }

    public void setLongArray(Long[] longArray) {
        this.longArray = longArray;
    }

    public String getEmojiBear() {
        return emojiBear;
    }

    public void setEmojiBear(String emojiBear) {
        this.emojiBear = emojiBear;
    }
}
