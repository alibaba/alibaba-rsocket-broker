package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.AlibabaRSocketBrokerServer;
import com.alibaba.rsocket.broker.web.model.Pair;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.alibaba.rsocket.broker.web.ui.SystemView.NAV;

/**
 * system view
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
public class SystemView extends VerticalLayout {
    public static final String NAV = "systemView";

    public SystemView() {
        setAlignItems(Alignment.CENTER);
        Div infoDiv = new Div();
        infoDiv.add(new H3("JVM"));
        Grid<Pair> appMetadataGrid = new Grid<>();
        appMetadataGrid.setItems(systemInfo());
        appMetadataGrid.addColumn(Pair::getName).setHeader("Name");
        appMetadataGrid.addColumn(Pair::getValue).setHeader("Value");
        appMetadataGrid.setWidth("1280px");
        infoDiv.add(appMetadataGrid);
        add(infoDiv);
    }


    public List<Pair> systemInfo() {
        List<Pair> pairs = new ArrayList<>();
        pairs.add(new Pair("OS Name", System.getProperty("os.name")));
        pairs.add(new Pair("OS Arch", System.getProperty("os.arch")));
        pairs.add(new Pair("OS Version", System.getProperty("os.version")));
        pairs.add(new Pair("Java VM Version", System.getProperty("java.runtime.version")));
        pairs.add(new Pair("VM Vendor Name", System.getProperty("java.vm.vendor")));
        pairs.add(new Pair("Java Version", System.getProperty("java.version")));
        pairs.add(new Pair("Java Home", System.getProperty("java.home")));
        pairs.add(new Pair("Java Classpath", System.getProperty("java.class.path")));
        pairs.add(new Pair("Java Boot Class Path", System.getProperty("sun.boot.class.path")));
        pairs.add(new Pair("Java Command", System.getProperty("sun.java.command")));
        pairs.add(new Pair("Started Time", AlibabaRSocketBrokerServer.STARTED_AT.format(DateTimeFormatter.ISO_DATE_TIME)));
        pairs.add(new Pair("Current Time", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)));
        return pairs;
    }
}
