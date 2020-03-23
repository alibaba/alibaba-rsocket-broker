package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.dns.Answer;
import com.alibaba.rsocket.broker.dns.DnsResolveService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.netty.handler.codec.dns.DnsRecordType;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

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
    private Grid<Tuple2<String, String>> domainNameGrid = new Grid<>();

    public DNSView(DnsResolveService resolveService) {
        this.resolveService = resolveService;
        add(new H1("Domain List"));
        domainNameGrid.addColumn(Tuple2::getT1).setHeader("Name");
        domainNameGrid.addColumn(Tuple2::getT2).setHeader("A");
        add(domainNameGrid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.domainNameGrid.setItems(domains());
    }

    @SuppressWarnings("ConstantConditions")
    public List<Tuple2<String, String>> domains() {
        return resolveService.allDomains()
                .map(name -> Tuples.of(name, String.join(", ", resolveService.resolve(name, DnsRecordType.A.name()).map(Answer::getData).collectList().block())))
                .collectList().block();
    }

}
