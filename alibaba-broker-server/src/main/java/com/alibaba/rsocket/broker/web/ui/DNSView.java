package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.dns.Answer;
import com.alibaba.rsocket.broker.dns.DnsResolveService;
import com.alibaba.rsocket.broker.web.model.Pair;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.netty.handler.codec.dns.DnsRecordType;

import java.util.List;

import static com.alibaba.rsocket.broker.web.ui.DNSView.NAV;


/**
 * DNS view
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
public class DNSView extends VerticalLayout {
    public static final String NAV = "dnsView";
    private DnsResolveService resolveService;

    public DNSView(DnsResolveService resolveService) {
        this.resolveService = resolveService;
        add(new H1("Domain List"));
        Grid<Pair> domainNameGrid = new Grid<>();
        domainNameGrid.setItems(domains());
        domainNameGrid.addColumn(Pair::getName).setHeader("Name");
        domainNameGrid.addColumn(Pair::getValue).setHeader("A");
        add(domainNameGrid);
    }

    @SuppressWarnings("ConstantConditions")
    public List<Pair> domains() {
        return resolveService.allDomains()
                .map(name -> new Pair(name, String.join(", ", resolveService.resolve(name, DnsRecordType.A.name()).map(Answer::getData).collectList().block())))
                .collectList().block();
    }

}
