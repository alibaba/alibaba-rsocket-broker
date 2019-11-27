package com.alibaba.rsocket.broker.dns;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * DNS response
 *
 * @author leijuan
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DnsResponse {
    /**
     * Standard DNS response code, 0(NOERROR)
     */
    @JsonProperty("Status")
    private Integer status = 0;
    /**
     * Whether the response is truncated
     */
    @JsonProperty("TC")
    private boolean tc = false;
    /**
     * Always true for RSocket Broker Public DNS
     */
    @JsonProperty("RD")
    private boolean rd = true;
    /**
     * Always true for RSocket Broker Public DNS
     */
    @JsonProperty("RA")
    private boolean ra = true;
    /**
     * Whether all response data was validated with DNSSEC
     */
    @JsonProperty("AD")
    private boolean ad = false;
    /**
     * Whether the client asked to disable DNSSEC
     */
    @JsonProperty("CD")
    private boolean cd;
    @JsonProperty("Comment")
    private String comment;
    @JsonProperty("Question")
    private List<Question> questions;
    @JsonProperty("Answer")
    private List<Answer> answers;
    /**
     * Authority for NS, PTR etc
     */
    @JsonProperty("Authority")
    private List<Answer> authorities;
    @JsonProperty("Additional")
    private List<Object> additional;
    @JsonProperty("edns_client_subnet")
    private String ednsClientSubnet;

    public DnsResponse() {
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public boolean isTc() {
        return tc;
    }

    public void setTc(boolean tc) {
        this.tc = tc;
    }

    public boolean isRd() {
        return rd;
    }

    public void setRd(boolean rd) {
        this.rd = rd;
    }

    public boolean isRa() {
        return ra;
    }

    public void setRa(boolean ra) {
        this.ra = ra;
    }

    public boolean isAd() {
        return ad;
    }

    public void setAd(boolean ad) {
        this.ad = ad;
    }

    public boolean isCd() {
        return cd;
    }

    public void setCd(boolean cd) {
        this.cd = cd;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void addQuestion(Question question) {
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
        questions.add(question);
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }

    public void addAnswer(Answer answer) {
        if (this.answers == null) {
            this.answers = new ArrayList<>();
        }
        this.answers.add(answer);
    }

    public List<Answer> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<Answer> authorities) {
        this.authorities = authorities;
    }

    public void addAuthority(Answer authority) {
        if (this.authorities == null) {
            this.authorities = new ArrayList<>();
        }
        this.authorities.add(authority);
    }

    public List<Object> getAdditional() {
        return additional;
    }

    public void setAdditional(List<Object> additional) {
        this.additional = additional;
    }

    public String getEdnsClientSubnet() {
        return ednsClientSubnet;
    }

    public void setEdnsClientSubnet(String ednsClientSubnet) {
        this.ednsClientSubnet = ednsClientSubnet;
    }
}
