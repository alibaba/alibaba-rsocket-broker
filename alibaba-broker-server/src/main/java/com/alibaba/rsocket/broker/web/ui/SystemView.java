package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.AlibabaRSocketBrokerServer;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

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
        Grid<Tuple2<String, String>> appMetadataGrid = new Grid<>();
        appMetadataGrid.setItems(systemInfo());
        appMetadataGrid.addColumn(Tuple2::getT1).setHeader("Name");
        appMetadataGrid.addColumn(Tuple2::getT2).setHeader("Value");
        appMetadataGrid.setWidth("1280px");
        infoDiv.add(appMetadataGrid);
        add(infoDiv);
    }


    public List<Tuple2<String, String>> systemInfo() {
        List<Tuple2<String, String>> pairs = new ArrayList<>();
        pairs.add(Tuples.of("OS Name", System.getProperty("os.name")));
        pairs.add(Tuples.of("OS Arch", System.getProperty("os.arch")));
        pairs.add(Tuples.of("OS Version", System.getProperty("os.version")));
        pairs.add(Tuples.of("Java VM Version", System.getProperty("java.runtime.version")));
        pairs.add(Tuples.of("VM Vendor Name", System.getProperty("java.vm.vendor")));
        pairs.add(Tuples.of("Java Version", System.getProperty("java.version")));
        pairs.add(Tuples.of("Java Home", System.getProperty("java.home")));
        pairs.add(Tuples.of("Started Time", AlibabaRSocketBrokerServer.STARTED_AT.format(DateTimeFormatter.ISO_DATE_TIME)));
        return pairs;
    }
}
