package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.route.RSocketFilter;
import com.alibaba.rsocket.route.RSocketFilterChain;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import static com.alibaba.rsocket.broker.web.ui.RSocketFiltersView.NAV;


/**
 * RSocket filter view
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
public class RSocketFiltersView extends VerticalLayout {
    public static final String NAV = "filtersView";

    public RSocketFiltersView(@Autowired RSocketFilterChain filterChain) {
        add(new H1("RSocket Filters"));
        //services & applications
        Grid<RSocketFilter> filterGrid = new Grid<>();
        filterGrid.setItems(filterChain.getFilters());
        filterGrid.addColumn(RSocketFilter::getClass).setHeader("Filter Class").setAutoWidth(true);
        filterGrid.addColumn(RSocketFilter::name).setHeader("Name");
        add(filterGrid);
    }

}
