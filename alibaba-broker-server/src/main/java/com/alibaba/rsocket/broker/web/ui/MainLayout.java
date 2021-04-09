package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.dns.DnsResolveService;
import com.alibaba.rsocket.route.RSocketFilterChain;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.alibaba.spring.boot.rsocket.broker.security.AuthenticationService;
import com.alibaba.spring.boot.rsocket.broker.services.ConfigurationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;

import static com.vaadin.flow.component.icon.VaadinIcon.*;

/**
 * main app router layout
 *
 * @author leijuan
 */
public class MainLayout extends AppLayout implements DisposableBean {
    private static final long serialVersionUID = -1741672705639398634L;
    private Map<Tab, Component> tab2Workspace = new HashMap<>();
    private RSocketBrokerHandlerRegistry handlerRegistry;
    private ServiceRoutingSelector serviceRoutingSelector;
    private RSocketBrokerManager rSocketBrokerManager;
    private DnsResolveService resolveService;
    private ConfigurationService configurationService;
    private AuthenticationService authenticationService;
    private RSocketFilterChain filterChain;
    private Sinks.Many<String> notificationProcessor;
    private Disposable notificationSubscribe = null;

    public MainLayout(@Autowired RSocketBrokerHandlerRegistry handlerRegistry,
                      @Autowired ServiceRoutingSelector serviceRoutingSelector,
                      @Autowired RSocketBrokerManager rSocketBrokerManager,
                      @Autowired DnsResolveService resolveService,
                      @Autowired ConfigurationService configurationService,
                      @Autowired AuthenticationService authenticationService,
                      @Autowired RSocketFilterChain filterChain,
                      @Autowired @Qualifier("notificationProcessor") Sinks.Many<String> notificationProcessor) {
        this.handlerRegistry = handlerRegistry;
        this.serviceRoutingSelector = serviceRoutingSelector;
        this.rSocketBrokerManager = rSocketBrokerManager;
        this.resolveService = resolveService;
        this.configurationService = configurationService;
        this.authenticationService = authenticationService;
        this.filterChain = filterChain;
        this.notificationProcessor = notificationProcessor;
        //init the Layout
        Image logo = new Image("/rsocket-logo.svg", "RSocket Logo");
        logo.setHeight("44px");
        logo.setAlt("RSocket Cluster");
        addToNavbar(new DrawerToggle(), logo);

        final Tabs tabs = new Tabs(dashBoard(),
                apps(),
                dns(),
                appConfig(),
                services(),
                serviceTesting(),
                //serviceMesh(),
                brokers(),
                filters(),
                jwt(),
                system(),
                faq()
        );
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.addSelectedChangeListener(event -> {
            final Tab selectedTab = event.getSelectedTab();
            final Component component = tab2Workspace.get(selectedTab);
            setContent(component);
        });
        addToDrawer(tabs);
        setContent(new Span("click in the menu ;-) , you will see me never again.."));
    }

    private Tab dashBoard() {
        final Span label = new Span("Dashboard");
        final Icon icon = DASHBOARD.create();
        final Tab tab = new Tab(new HorizontalLayout(icon, label));
        tab2Workspace.put(tab, new DashboardView(this.handlerRegistry, this.serviceRoutingSelector, this.rSocketBrokerManager));
        return tab;
    }

    private Tab apps() {
        final Span label = new Span("Apps");
        final Icon icon = BULLETS.create();
        final Tab tab = new Tab(new HorizontalLayout(icon, label));
        tab2Workspace.put(tab, new AppsView(this.handlerRegistry));
        return tab;
    }

    private Tab dns() {
        final Span label = new Span("DNS");
        final Icon icon = RECORDS.create();
        final Tab tab = new Tab(new HorizontalLayout(icon, label));
        tab2Workspace.put(tab, new DNSView(this.resolveService));
        return tab;
    }

    private Tab appConfig() {
        final Span label = new Span("AppConfig");
        final Icon icon = DATABASE.create();
        final Tab tab = new Tab(new HorizontalLayout(icon, label));
        tab2Workspace.put(tab, new AppConfigView(this.configurationService, this.rSocketBrokerManager));
        return tab;
    }

    private Tab services() {
        final Span label = new Span("Services");
        final Icon icon = BULLETS.create();
        final Tab tab = new Tab(new HorizontalLayout(icon, label));
        tab2Workspace.put(tab, new ServicesView(this.handlerRegistry, this.serviceRoutingSelector));
        return tab;
    }

    private Tab serviceMesh() {
        final Span label = new Span("ServiceMesh");
        final Icon icon = CLUSTER.create();
        final Tab tab = new Tab(new HorizontalLayout(icon, label));
        tab2Workspace.put(tab, new ServiceMeshView(this.handlerRegistry));
        return tab;
    }


    private Tab brokers() {
        final Span label = new Span("Brokers");
        final Icon icon = CUBES.create();
        final Tab tab = new Tab(new HorizontalLayout(icon, label));
        tab2Workspace.put(tab, new BrokersView(this.rSocketBrokerManager));
        return tab;
    }

    private Tab system() {
        final Span label = new Span("Server");
        final Icon icon = SERVER.create();
        final Tab tab = new Tab(new HorizontalLayout(icon, label));
        tab2Workspace.put(tab, new SystemView());
        return tab;
    }

    private Tab serviceTesting() {
        final Span label = new Span("Service Testing");
        final Icon icon = TOOLS.create();
        final Tab tab = new Tab(new HorizontalLayout(icon, label));
        tab2Workspace.put(tab, new ServiceTestingView(this.handlerRegistry, this.serviceRoutingSelector));
        return tab;
    }

    private Tab faq() {
        final Span label = new Span("FAQ");
        final Icon icon = QUESTION.create();
        final Tab tab = new Tab(new HorizontalLayout(icon, label));
        tab2Workspace.put(tab, new FAQView());
        return tab;
    }

    private Tab jwt() {
        final Span label = new Span("JWT");
        final Icon icon = PASSWORD.create();
        final Tab tab = new Tab(new HorizontalLayout(icon, label));
        tab2Workspace.put(tab, new JwtGeneratorView(this.authenticationService));
        return tab;
    }


    private Tab filters() {
        final Span label = new Span("RSocket Filters");
        final Icon icon = FORWARD.create();
        final Tab tab = new Tab(new HorizontalLayout(icon, label));
        tab2Workspace.put(tab, new RSocketFiltersView(this.filterChain, this.rSocketBrokerManager));
        return tab;
    }

    @Override
    public void destroy() throws Exception {
        for (Component value : tab2Workspace.values()) {
            if (value instanceof DisposableBean) {
                ((DisposableBean) value).destroy();
            }
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        closeSubscribeQuietly();
        this.notificationSubscribe = this.notificationProcessor.asFlux().subscribe(text -> {
            attachEvent.getUI().access(() -> {
                Notification.show(text);
            });
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        closeSubscribeQuietly();
    }

    private void closeSubscribeQuietly() {
        if (this.notificationSubscribe != null) {
            notificationSubscribe.dispose();
            notificationSubscribe = null;
        }
    }
}
