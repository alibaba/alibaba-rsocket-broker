package com.alibaba.rsocket.broker.dns.impl;

import com.alibaba.rsocket.broker.dns.Answer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * DnsResolveServiceImpl test
 *
 * @author leijuan
 */
public class DnsResolveServiceImplTest {
    private DnsResolveServiceImpl dnsResolveService = new DnsResolveServiceImpl();

    @Test
    public void testAddRecord() {
        dnsResolveService.addRecords("www.tmall.com", "A", "192.168.1.2", "192.168.1.3");
        Set<Answer> records = dnsResolveService.resolve("www.tmall.com", "A").toStream().collect(Collectors.toSet());
        Assertions.assertThat(records).hasSize(2);
    }

}
