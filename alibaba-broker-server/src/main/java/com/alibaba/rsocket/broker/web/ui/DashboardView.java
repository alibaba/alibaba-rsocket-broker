package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.web.ui.component.Panel;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import io.micrometer.core.instrument.Metrics;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

import static com.alibaba.rsocket.broker.web.ui.DashboardView.NAV;

/**
 * dashboard view
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class DashboardView extends VerticalLayout {
    public static final String NAV = "dashboard";
    private Text brokersCount = new Text("0");
    private Text appsCount = new Text("0");
    private Text servicesCount = new Text("0");
    private Text connectionsCount = new Text("0");
    private Text requestsCounter = new Text("0");
    private Grid<AppMetadata> appMetadataGrid = new Grid<>();

    private RSocketBrokerHandlerRegistry handlerRegistry;
    private ServiceRoutingSelector serviceRoutingSelector;
    private RSocketBrokerManager rSocketBrokerManager;

    public DashboardView(@Autowired RSocketBrokerHandlerRegistry handlerRegistry,
                         @Autowired ServiceRoutingSelector serviceRoutingSelector,
                         @Autowired RSocketBrokerManager rSocketBrokerManager) {
        this.handlerRegistry = handlerRegistry;
        this.serviceRoutingSelector = serviceRoutingSelector;
        this.rSocketBrokerManager = rSocketBrokerManager;
        setAlignItems(Alignment.CENTER);
        //---- top
        HorizontalLayout top = new HorizontalLayout();
        top.setAlignItems(Alignment.CENTER);
        add(top);
        // brokers panel
        Panel brokersPanel = new Panel("Brokers");
        brokersPanel.add(brokersCount);
        top.add(brokersPanel);
        // apps panel
        Panel appsPanel = new Panel("Apps");
        appsPanel.add(appsCount);
        top.add(appsPanel);
        // services panel
        Panel servicePanel = new Panel("Services");
        servicePanel.add(servicesCount);
        top.add(servicePanel);
        // connections panel
        Panel connectionPanel = new Panel("Connections");
        connectionPanel.add(connectionsCount);
        top.add(connectionPanel);
        // requests count panel
        Panel requestsPanel = new Panel("Requests");
        requestsPanel.add(requestsCounter);
        top.add(requestsPanel);
        //--- last ten apps
        Div div2 = new Div();
        div2.add(new H3("Last apps"));
        appMetadataGrid.addColumn(AppMetadata::getName).setHeader("App Name");
        appMetadataGrid.addColumn(AppMetadata::getIp).setHeader("IP");
        appMetadataGrid.addColumn(AppMetadata::getConnectedAt).setHeader("Timestamp");
        appMetadataGrid.setWidth("1024px");
        div2.add(appMetadataGrid);
        add(div2);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        String brokerClusterType = rSocketBrokerManager.getName();
        this.brokersCount.setText(rSocketBrokerManager.currentBrokers().size() + " (" + brokerClusterType + ")");
        this.appsCount.setText(String.valueOf(handlerRegistry.appHandlers().size()));
        this.servicesCount.setText(String.valueOf(serviceRoutingSelector.findAllServices().size()));
        this.connectionsCount.setText(String.valueOf(handlerRegistry.findAll().size()));
        this.requestsCounter.setText(metricsCounterValue("rsocket.request.counter"));
        this.appMetadataGrid.setItems(appMetadataList(handlerRegistry));
    }

    public List<AppMetadata> appMetadataList(RSocketBrokerHandlerRegistry handlerFactory) {
        return handlerFactory.findAll()
                .stream()
                .map(RSocketBrokerResponderHandler::getAppMetadata)
                .sorted((o1, o2) -> o2.getConnectedAt().compareTo(o1.getConnectedAt()))
                .limit(10)
                .collect(Collectors.toList());
    }

    public String metricsCounterValue(String name) {
        return String.valueOf(((long) (Metrics.counter(name).count())));
    }
}
