package com.alibaba.rsocket.broker.dns;

import com.alibaba.rsocket.RSocketAppContext;
import io.netty.handler.codec.dns.DnsRecordType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * DNS-over-HTTP controller
 *
 * @author leijuan
 */
@RestController
public class DnsOverHTTPController {
    @Autowired
    private DnsResolveService dnsResolveService;

    @RequestMapping("/resolve")
    public Mono<DnsResponse> query(@RequestParam("name") String name,
                                   @RequestParam(name = "type", required = false, defaultValue = "A") String type,
                                   @RequestParam(name = "cd", required = false, defaultValue = "false") boolean cd,
                                   @RequestParam(name = "ct", required = false, defaultValue = "") String ct,
                                   @RequestParam(name = "do", required = false, defaultValue = "false") boolean do1,
                                   @RequestParam(name = "edns_client_subnet", required = false, defaultValue = "") String ednsClientSubnet,
                                   @RequestParam(name = "random_padding", required = false, defaultValue = "") String randomPadding
    ) {
        String appName = name.endsWith(".") ? name.substring(0, name.length() - 1) : name;
        DnsRecordType dnsRecordType = DnsRecordType.valueOf(type.toUpperCase());
        DnsResponse response = new DnsResponse();
        response.addQuestion(new Question(name, dnsRecordType.intValue()));
        response.setComment("Response from the broker " + RSocketAppContext.ID);
        return dnsResolveService.resolve(appName, type).collectList().map(answers -> {
            response.setAnswers(answers);
            response.setStatus(0);
            return response;
        }).switchIfEmpty(Mono.fromCallable(() -> {
            response.setStatus(-1);
            return response;
        }));
    }
}
