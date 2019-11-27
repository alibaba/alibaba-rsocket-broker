package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.web.ui.component.Panel;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.micrometer.core.instrument.Metrics;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

import static com.alibaba.rsocket.broker.web.ui.DashboardView.NAV;

/**
 * dashboard view
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
public class DashboardView extends VerticalLayout implements DisposableBean {
    public static final String NAV = "";
    private Disposable timerSubscriber;

    public DashboardView(@Autowired RSocketBrokerHandlerRegistry handlerRegistry,
                         @Autowired ServiceRoutingSelector serviceRoutingSelector,
                         @Autowired RSocketBrokerManager rSocketBrokerManager,
                         Flux<Long> fiveSecondsTimer) {
        setAlignItems(Alignment.CENTER);
        //---- top
        HorizontalLayout top = new HorizontalLayout();
        top.setAlignItems(Alignment.CENTER);
        add(top);
        // brokers panel
        Panel brokersPanel = new Panel("Brokers");
        brokersPanel.add(new Text(String.valueOf(rSocketBrokerManager.currentBrokers().size())));
        top.add(brokersPanel);
        // apps panel
        Panel appsPanel = new Panel("Apps");
        Text appsText = new Text(String.valueOf(handlerRegistry.appHandlers().size()));
        appsPanel.add(appsText);
        top.add(appsPanel);
        // services panel
        Panel servicePanel = new Panel("Services");
        Text servicesText = new Text(String.valueOf(serviceRoutingSelector.findAllServices().size()));
        servicePanel.add(servicesText);
        top.add(servicePanel);
        // connections panel
        Panel connectionPanel = new Panel("Connections");
        Text connectionsText = new Text(String.valueOf(handlerRegistry.findAll().size()));
        connectionPanel.add(connectionsText);
        top.add(connectionPanel);
        // requests count panel
        Panel requestsPanel = new Panel("Requests");
        Text requestsCounter = new Text(metricsCounterValue("rsocket.request.count"));
        requestsPanel.add(requestsCounter);
        top.add(requestsPanel);
        //subscribe and update statics
        timerSubscriber = fiveSecondsTimer.subscribe(timestamp -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                requestsCounter.setText(metricsCounterValue("rsocket.request.count"));
                servicesText.setText(String.valueOf(serviceRoutingSelector.findAllServices().size()));
                appsText.setText(String.valueOf(handlerRegistry.appHandlers().size()));
                connectionsText.setText(String.valueOf(handlerRegistry.findAll().size()));
            }));
        });
        //--- last ten apps
        Div div2 = new Div();
        div2.add(new H3("Last apps"));
        Grid<AppMetadata> appMetadataGrid = new Grid<>();
        appMetadataGrid.setItems(appMetadataList(handlerRegistry));
        appMetadataGrid.addColumn(AppMetadata::getName).setHeader("App Name");
        appMetadataGrid.addColumn(AppMetadata::getIp).setHeader("IP");
        appMetadataGrid.addColumn(AppMetadata::getConnectedAt).setHeader("Timestamp");
        appMetadataGrid.setWidth("1024px");
        div2.add(appMetadataGrid);
        add(div2);
    }

    public List<AppMetadata> appMetadataList(RSocketBrokerHandlerRegistry handlerFactory) {
        return handlerFactory.findAll()
                .stream()
                .limit(10)
                .map(RSocketBrokerResponderHandler::getAppMetadata)
                .collect(Collectors.toList());
    }

    public String metricsCounterValue(String name) {
        return String.valueOf(((long) (Metrics.counter(name).count())));
    }

    @Override
    public void destroy() throws Exception {
        if (timerSubscriber != null) {
            timerSubscriber.dispose();
        }
    }
}
