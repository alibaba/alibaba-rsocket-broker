package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.web.model.AppTrafficAccess;
import com.alibaba.rsocket.metadata.AppMetadata;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

import static com.alibaba.rsocket.broker.web.ui.ServiceMeshView.NAV;

/**
 * ServiceMesh view to config SMI
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
public class ServiceMeshView extends VerticalLayout {
    public static final String NAV = "ServiceMeshView";
    private Grid<AppTrafficAccess> trafficAccessGrid = new Grid<>();
    private RSocketBrokerHandlerRegistry handlerRegistry;

    public ServiceMeshView(@Autowired RSocketBrokerHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
        add(new H1("Service Mesh"));
        trafficAccessGrid.addColumn(AppTrafficAccess::getAppName).setHeader("App Name");
        trafficAccessGrid.addColumn(appTrafficAccess -> appTrafficAccess.getOrgs() + ":" + appTrafficAccess.getServiceAccounts()).setHeader("Service Accounts");
        trafficAccessGrid.addColumn(appTrafficAccess -> "").setHeader("Internal Services");
        trafficAccessGrid.addColumn(appTrafficAccess -> "com.alibaba.item.ItemService").setHeader("Granted Services");
        add(trafficAccessGrid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.trafficAccessGrid.setItems(appTrafficAccesses(handlerRegistry));
    }

    public List<AppTrafficAccess> appTrafficAccesses(RSocketBrokerHandlerRegistry handlerFactory) {
        return handlerFactory.findAll()
                .stream()
                .map(handler -> {
                    AppTrafficAccess trafficAccess = new AppTrafficAccess();
                    AppMetadata appMetadata = handler.getAppMetadata();
                    trafficAccess.setAppName(appMetadata.getName());
                    trafficAccess.setOrgs(appMetadata.getMetadata("_orgs"));
                    trafficAccess.setServiceAccounts(appMetadata.getMetadata("_serviceAccounts"));
                    return trafficAccess;
                })
                .collect(Collectors.toList());
    }
}
