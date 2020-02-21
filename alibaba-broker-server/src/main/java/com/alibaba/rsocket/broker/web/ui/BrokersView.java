package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBroker;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import static com.alibaba.rsocket.broker.web.ui.BrokersView.NAV;

/**
 * brokers view
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
public class BrokersView extends VerticalLayout {
    public static final String NAV = "brokersView";
    private Grid<RSocketBroker> brokerDataGrid = new Grid<>();
    private RSocketBrokerManager brokerManager;

    public BrokersView(@Autowired RSocketBrokerManager brokerManager) {
        this.brokerManager = brokerManager;
        add(new H1("Broker List"));
        brokerDataGrid.addColumn(RSocketBroker::getIp).setHeader("IP");
        brokerDataGrid.addColumn(RSocketBroker::getPort).setHeader("Port");
        brokerDataGrid.addColumn(RSocketBroker::getUrl).setHeader("Link");
        add(brokerDataGrid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        brokerDataGrid.setItems(brokerManager.currentBrokers());
    }
}
