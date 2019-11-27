package com.alibaba.rsocket.broker.dns.impl;

import com.alibaba.rsocket.broker.dns.Answer;
import com.alibaba.rsocket.broker.dns.DnsResponse;
import com.alibaba.rsocket.broker.dns.Question;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * DNS Response test
 *
 * @author leijuan
 */
public class DnsResponseTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testJsonOutput() throws Exception {
        DnsResponse response = new DnsResponse();
        response.setComment("Response from 192.168.1.3");
        response.addQuestion(new Question("www.taobao.com", 1));
        response.addAnswer(new Answer("www.taobao.com", 1, 559, "192.168.1.2"));
        System.out.println(objectMapper.writeValueAsString(response));
    }
}
