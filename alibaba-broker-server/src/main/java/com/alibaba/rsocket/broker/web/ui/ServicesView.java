package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.web.model.ServiceInfo;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.micrometer.core.instrument.Metrics;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

import static com.alibaba.rsocket.broker.web.ui.ServicesView.NAV;

/**
 * Services view
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
public class ServicesView extends VerticalLayout {
    public static final String NAV = "servicesView";
    private final RSocketBrokerHandlerRegistry handlerRegistry;
    private final ServiceRoutingSelector routingSelector;
    private Grid<ServiceInfo> servicesGrid = new Grid<>();

    public ServicesView(@Autowired RSocketBrokerHandlerRegistry handlerRegistry, @Autowired ServiceRoutingSelector routingSelector) {
        this.handlerRegistry = handlerRegistry;
        this.routingSelector = routingSelector;
        add(new H1("Service List"));
        //services & applications
        servicesGrid.addColumn(ServiceInfo::getGroup).setHeader("Group");
        servicesGrid.addColumn(ServiceInfo::getService).setHeader("Service").setAutoWidth(true);
        servicesGrid.addColumn(ServiceInfo::getVersion).setHeader("Version");
        servicesGrid.addColumn(ServiceInfo::getServiceIdHex).setHeader("ID");
        servicesGrid.addColumn(ServiceInfo::getCounter).setHeader("Counter");
        servicesGrid.addColumn(ServiceInfo::getInstances).setHeader("Instances");
        servicesGrid.addColumn(ServiceInfo::getOrgs).setHeader("Orgs");
        servicesGrid.addColumn(ServiceInfo::getServiceAccounts).setHeader("ServiceAccounts");
        add(servicesGrid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        servicesGrid.setItems(services(handlerRegistry, routingSelector));
    }

    public List<ServiceInfo> services(RSocketBrokerHandlerRegistry handlerRegistry, ServiceRoutingSelector routingSelector) {
        return routingSelector.findAllServices()
                .stream()
                .map(serviceLocator -> {
                    ServiceInfo serviceInfo = new ServiceInfo(serviceLocator.getGroup(), serviceLocator.getService(), serviceLocator.getVersion(),
                            ((long) (Metrics.counter(serviceLocator.getService() + ".counter").count())),
                            routingSelector.getInstanceCount(serviceLocator.getId()));
                    //serviceInfo.setOrgs(String.join(",", serviceRoutingSelector.getServiceOrgs(serviceLocator.getId())));
                    //serviceInfo.setServiceAccounts(String.join(",", serviceRoutingSelector.getServiceAccounts(serviceLocator.getId())));
                    return serviceInfo;
                }).collect(Collectors.toList());
    }
}
