package com.alibaba.rsocket.broker.web.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;

/**
 * App Detail Panel
 *
 * @author leijuan
 */
public class AppDetailPanel extends Div {
    private H3 title = new H3("App Detail");
    private Paragraph description = new Paragraph();
    private Pre humans = new Pre();

    public AppDetailPanel() {
        add(title);
        add(description);
        add(humans);
    }

    public void setAppName(String appName) {
        this.title.setText("App Detail: " + appName);
    }

    public void setDescription(String description) {
        this.description.setText(description);
    }

    public void setHumans(String humans) {
        this.humans.setText(humans);
    }


}
