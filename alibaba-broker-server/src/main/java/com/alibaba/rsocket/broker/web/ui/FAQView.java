package com.alibaba.rsocket.broker.web.ui;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import static com.alibaba.rsocket.broker.web.ui.FAQView.NAV;


/**
 * FAQ view
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
public class FAQView extends VerticalLayout {
    public static final String NAV = "FAQView";

    public FAQView() {
        add(new H1("FAQ"));
        Accordion accordion = new Accordion();
        accordion.add("RSocket Broker是如何控制服务调用的？", new Paragraph("RSocket Broker采用org和Service Account来控制服务调用，只有相同org和service account才能相互访问，如果不是则需要进行ACL访问授权！"));
        accordion.add("RSocket Broker的集群是如何管理？", new Paragraph("目前主要是基于Gossip进行集群管理"));
        accordion.add("RSocket Broker支持DNS吗？", new Paragraph("RSocket支持DNS-over-HTTP和DNS-over-RSocket，OkHttp3默认支持！"));
        accordion.add("RSocket的相关资料有吗？", new Paragraph("请访问 http://rsocketbyexample.info 和 https://github.com/alibaba/alibaba-rsocket-broker/wiki"));
        add(accordion);
    }

}
