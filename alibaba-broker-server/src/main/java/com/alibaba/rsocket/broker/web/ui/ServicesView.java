package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.web.model.ServiceInfo;
import com.alibaba.rsocket.utils.MurmurHash3;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
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

    public ServicesView(@Autowired RSocketBrokerHandlerRegistry handlerRegistry, @Autowired ServiceRoutingSelector routingSelector) {
        add(new H1("Service List"));
        //services & applications
        Grid<ServiceInfo> servicesGrid = new Grid<>();
        servicesGrid.setItems(services(handlerRegistry, routingSelector));
        servicesGrid.addColumn(ServiceInfo::getGroup).setHeader("Group");
        servicesGrid.addColumn(ServiceInfo::getService).setHeader("Service");
        servicesGrid.addColumn(ServiceInfo::getVersion).setHeader("Version");
        servicesGrid.addColumn(ServiceInfo::getCounter).setHeader("Counter");
        servicesGrid.addColumn(ServiceInfo::getInstances).setHeader("Instances");
        servicesGrid.addColumn(ServiceInfo::getOrgs).setHeader("Orgs");
        servicesGrid.addColumn(ServiceInfo::getServiceAccounts).setHeader("ServiceAccounts");
        add(servicesGrid);
    }

    public List<ServiceInfo> services(RSocketBrokerHandlerRegistry handlerRegistry, ServiceRoutingSelector routingSelector) {
        return routingSelector.findAllServices()
                .stream()
                .map(serviceId -> {
                    ServiceInfo serviceInfo;
                    if (serviceId.contains(":")) {
                        String[] parts = serviceId.split(":");
                        serviceInfo = new ServiceInfo(parts[0], parts[1], parts.length > 2 ? parts[2] : "",
                                ((long) (Metrics.counter(parts[1] + ".counter").count())),
                                routingSelector.getInstanceCount(MurmurHash3.hash32(serviceId)));

                    } else {
                        serviceInfo = new ServiceInfo("", serviceId, "",
                                (long) Metrics.counter(serviceId + ".counter").count(),
                                routingSelector.getInstanceCount(MurmurHash3.hash32(serviceId)));
                    }
                    //serviceInfo.setOrgs(Joiner.on(",").join(serviceRoutingSelector.getServiceOrgs(serviceId)));
                    //serviceInfo.setServiceAccounts(Joiner.on(",").join(serviceRoutingSelector.getServiceAccounts(serviceId)));
                    return serviceInfo;
                }).collect(Collectors.toList());
    }
}
