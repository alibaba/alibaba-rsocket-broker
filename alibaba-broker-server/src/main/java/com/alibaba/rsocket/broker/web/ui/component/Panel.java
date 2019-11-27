package com.alibaba.rsocket.broker.web.ui.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;

/**
 * Panel
 *
 * @author linux_china
 */
public class Panel extends Div {
    public Panel(String title) {
        super();
        setClassName("panel");
        H4 h4 = new H4(title);
        h4.getElement().getStyle().set("color", "blue");
        add(h4);
        setWidth("200px");
    }

}
